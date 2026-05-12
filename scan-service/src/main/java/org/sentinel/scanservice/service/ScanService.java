package org.sentinel.scanservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.sentinel.scanservice.dto.ScanMapper;
import org.sentinel.scanservice.dto.ScanResponse;
import org.sentinel.scanservice.dto.ScanSubmissionResponse;
import org.sentinel.scanservice.exception.BadRequestException;
import org.sentinel.scanservice.exception.ForbiddenException;
import org.sentinel.scanservice.exception.NotFoundException;
import org.sentinel.scanservice.model.*;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.sentinel.scanservice.repo.ScanOutboxEventRepository;
import org.sentinel.scanservice.repo.ScanRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private final ScanCommandBuilder commandBuilder;
    private final ScanRequestRepository scanRequestRepository;
    private final ScanItemRepository scanItemRepository;
    private final ScanOutboxEventRepository outboxRepository;
    private final ScanMapper scanMapper;


    @Value("${topics.output-name}")
    private String scanCommandTopic;

    // One shared ObjectMapper with JavaTimeModule so Instant serializes as ISO-8601,
    // matching what the Kafka consumer on nmap-service expects.
    // Not a Spring bean — private to this service, no conflict risk.
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Transactional
    public ScanSubmissionResponse submitScan(ScanRequest scanRequest, UUID userId, String idempotencyKey) {
        log.info("Submitting scan. UserId: {}, Target: {}, IdempotencyKey: {}",
                userId, scanRequest.getTarget(), idempotencyKey);

        // Fast-path: same key seen before → return original response, no new scan
        if (idempotencyKey != null) {
            Optional<ScanItem> existing =
                    scanItemRepository.findByUserIdAndIdempotencyKey(userId, idempotencyKey);
            if (existing.isPresent()) {
                log.info("Duplicate submission. UserId: {}, Key: {}, Returning ScanItemId: {}",
                        userId, idempotencyKey, existing.get().getScanItemId());
                return scanMapper.toSubmissionResponse(existing.get().getScanItemId());
            }
        }

        // Step 1 — persist scan request (unchanged)
        ScanRequest saved = scanRequestRepository.save(scanRequest);
        List<String> cmd = commandBuilder.buildCommand(saved);
        String correlationId = UUID.randomUUID().toString();

        // Step 2 — persist scan item at ACCEPTED
        ScanItem scanItem = ScanItem.builder()
                .userId(userId)
                .scanRequestId(saved.getScanRequestId())
                .scanStatus(ScanStatus.ACCEPTED)
                .target(scanRequest.getTarget())
                .scanCommand(cmd)
                .correlationId(correlationId)
                .idempotencyKey(idempotencyKey)
                .build();
        scanItem = scanItemRepository.save(scanItem);

        // Step 3 — build the Kafka message payload
        ScanCommandMessage message = ScanCommandMessage.builder()
                .correlationId(correlationId)
                .userId(userId)
                .scanRequestId(saved.getScanRequestId())
                .scanItemId(scanItem.getScanItemId())
                .command(cmd)
                .target(scanRequest.getTarget())
                .timestamp(Instant.now())
                .scanMode(scanRequest.getScanMode())
                .protocol(scanRequest.getProtocol())
                .build();

        // Step 4 — write outbox row IN THE SAME TRANSACTION as step 2
        // If this transaction commits → both ScanItem and outbox row are durable.
        // If MySQL is down → both roll back, user gets 500, nothing is orphaned.
        // Kafka is NOT touched here at all.
        try {
            String payload = objectMapper.writeValueAsString(message);

            ScanOutboxEvent outboxEvent = ScanOutboxEvent.builder()
                    .scanItemId(scanItem.getScanItemId())
                    .topic(scanCommandTopic)
                    .messageKey(correlationId)
                    .payload(payload)
                    // published defaults to false via @Builder.Default
                    .build();

            outboxRepository.save(outboxEvent);

        } catch (Exception e) {
            // writeValueAsString only fails if message is not serializable —
            // ScanCommandMessage is a simple POJO, this should never happen.
            // If it does, we must fail fast and not accept the scan.
            log.error("Failed to serialize scan command for outbox. ScanItemId: {}",
                    scanItem.getScanItemId(), e);
            throw new RuntimeException("Failed to create scan job", e);
        }

        // Step 5 — status stays ACCEPTED until the outbox publisher confirms Kafka ack.
        // Do NOT set QUEUED here — QUEUED means "Kafka has it". We don't know that yet.
        log.info("Scan accepted and queued in outbox. ScanItemId: {}, CorrelationId: {}",
                scanItem.getScanItemId(), correlationId);

        return scanMapper.toSubmissionResponse(scanItem.getScanItemId());
        // Note: ScanMapper.toSubmissionResponse() returns status="ACCEPTED" —
        // this is now semantically correct (was "QUEUED" before which was premature).
    }

    public ScanResponse getScanDetails(UUID scanId, UUID userId) {
        ScanItem scanItem = getScanByIdWithAuthCheck(scanId, userId);
        return scanMapper.toDetailResponse(scanItem);
    }

    public ScanResponse getScanStatus(UUID scanId, UUID userId) {
        ScanItem scanItem = getScanByIdWithAuthCheck(scanId, userId);
        return scanMapper.toStatusResponse(scanItem);
    }

    public ScanResponse getScanResult(UUID scanId, UUID userId) {
        ScanItem scanItem = getScanByIdWithAuthCheck(scanId, userId);

        if (scanItem.getScanStatus() != ScanStatus.FINISHED) {
            throw new BadRequestException("Scan not completed. Status: " + scanItem.getScanStatus());
        }

        return scanMapper.toResultResponse(scanItem);
    }

    public List<ScanResponse> getUserScans(UUID userId, int limit, int page) {
        if (limit < 1 || limit > 100) {
            throw new BadRequestException("limit must be between 1 and 100");
        }
        if (page < 0) {
            throw new BadRequestException("page must not be negative");
        }
        List<ScanItem> scans = scanItemRepository
                .findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, limit));
        return scans.stream().map(scanMapper::toSummaryResponse).toList();
    }

    @Transactional
    public void deleteScan(UUID scanId, UUID userId) {
        ScanItem scanItem = getScanByIdWithAuthCheck(scanId, userId);

        if (scanItem.isInProgress()) {
            scanItem.setScanStatus(ScanStatus.CANCELLED);
            scanItem.setErrorMessage("Cancelled by user");
            scanItemRepository.save(scanItem);
            log.info("Scan cancelled: {}", scanId);
        } else {
            scanItemRepository.delete(scanItem);
            log.info("Scan deleted: {}", scanId);
        }
    }

    public ScanItem getScanByCorrelationId(String correlationId) {
        return scanItemRepository.findByCorrelationId(correlationId)
                .orElseThrow(() -> new NotFoundException("Scan not found: " + correlationId));
    }

    private ScanItem getScanById(UUID scanItemId) {
        return scanItemRepository.findById(scanItemId)
                .orElseThrow(() -> new NotFoundException("Scan not found: " + scanItemId));
    }

    private ScanItem getScanByIdWithAuthCheck(UUID scanId, UUID userId) {
        ScanItem scanItem = getScanById(scanId);

        if (!scanItem.getUserId().equals(userId)) {
            log.warn("Unauthorized access. ScanId: {}, UserId: {}", scanId, userId);
            throw new ForbiddenException("Access denied");
        }

        return scanItem;
    }
}