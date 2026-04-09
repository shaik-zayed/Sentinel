package org.sentinel.scan_service.service;

import lombok.extern.slf4j.Slf4j;
import lombok.RequiredArgsConstructor;
import org.sentinel.scan_service.dto.ScanMapper;
import org.sentinel.scan_service.dto.ScanResponse;
import org.sentinel.scan_service.dto.ScanSubmissionResponse;

import org.sentinel.scan_service.exception.BadRequestException;
import org.sentinel.scan_service.exception.ForbiddenException;
import org.sentinel.scan_service.exception.NotFoundException;
import org.sentinel.scan_service.model.*;
import org.sentinel.scan_service.repo.ScanItemRepository;
import org.sentinel.scan_service.repo.ScanRequestRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanService {

    private final ScanCommandBuilder commandBuilder;
    private final ScanRequestRepository scanRequestRepository;
    private final KafkaScanCommandProducer kafkaProducer;
    private final ScanItemRepository scanItemRepository;
    private final ScanMapper scanMapper;

    @Transactional
    public ScanSubmissionResponse submitScan(ScanRequest scanRequest, UUID userId) {
        log.info("Submitting scan. UserId: {}, Target: {}", userId, scanRequest.getTarget());

        ScanRequest saved = scanRequestRepository.save(scanRequest);
        List<String> cmd = commandBuilder.buildCommand(saved);

        String correlationId = UUID.randomUUID().toString();

        ScanItem scanItem = ScanItem.builder()
                .userId(userId)
                .scanRequestId(saved.getScanRequestId())
                .scanStatus(ScanStatus.ACCEPTED)
                .target(scanRequest.getTarget())
                .scanCommand(cmd)
                .correlationId(correlationId)
                .build();
        scanItem = scanItemRepository.save(scanItem);

        try {
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

            kafkaProducer.pushScanCommand(message);

            scanItem.setScanStatus(ScanStatus.QUEUED);
            scanItemRepository.save(scanItem);

            log.info("Scan submitted. ScanItemId: {}", scanItem.getScanItemId());
            return scanMapper.toSubmissionResponse(scanItem.getScanItemId());

        } catch (Exception e) {
            log.error("Failed to submit scan. ScanItemId: {}", scanItem.getScanItemId(), e);
            scanItem.setScanStatus(ScanStatus.FAILED);
            scanItem.setErrorMessage("Failed to queue scan: " + e.getMessage());
            scanItemRepository.save(scanItem);

            throw new RuntimeException("Failed to submit scan", e);
        }
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

    public List<ScanResponse> getUserScans(UUID userId, int limit, int offset) {
        List<ScanItem> scans = scanItemRepository.findByUserIdOrderByCreatedAtDesc(
                userId, PageRequest.of(offset / limit, limit));

        return scans.stream()
                .map(scanMapper::toSummaryResponse)
                .toList();
    }

    @Transactional
    public void deleteScan(UUID scanId, UUID userId) {
        ScanItem scanItem = getScanByIdWithAuthCheck(scanId, userId);

        if (scanItem.getScanStatus() == ScanStatus.QUEUED ||
                scanItem.getScanStatus() == ScanStatus.STARTED ||
                scanItem.getScanStatus() == ScanStatus.PENDING) {
            scanItem.setScanStatus(ScanStatus.FAILED);
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