package org.sentinel.scanservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.model.ScanResultMessage;
import org.sentinel.scanservice.model.ScanStatus;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaScanResultConsumer {

    private final ScanItemRepository scanItemRepository;
    private final AsyncEnrichmentExecutor asyncEnrichmentExecutor;

    @KafkaListener(
            topics = "${topics.input-name}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    @Transactional
    public void consumeScanResult(
            @Payload ScanResultMessage message,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received scan result from Kafka. CorrelationId: {}, UserId: {}, Success: {}, Partition: {}, Offset: {}",
                message.getCorrelationId(), message.getUserId(), message.isSuccess(), partition, offset);

        try {
            Optional<ScanItem> scanItemOpt = scanItemRepository
                    .findByCorrelationId(message.getCorrelationId());

            if (scanItemOpt.isEmpty()) {
                log.error("ORPHANED RESULT: No scan item found for correlationId: {}. " +
                                "This indicates a data consistency issue!",
                        message.getCorrelationId());
                return;
            }

            ScanItem scanItem = scanItemOpt.get();

            if (!scanItem.getUserId().equals(message.getUserId())) {
                log.error("SECURITY ALERT: User ID mismatch! " +
                                "ScanItem UserId: {}, Message UserId: {}. CorrelationId: {}. " +
                                "Possible data corruption or security breach!",
                        scanItem.getUserId(), message.getUserId(), message.getCorrelationId());
                return;
            }

            if (scanItem.getScanStatus() == ScanStatus.CANCELLED) {
                log.info("Result received for cancelled scan: {}. Ignoring — expected behaviour.",
                        message.getCorrelationId());
                return;
            }

            if (scanItem.getScanStatus() == ScanStatus.FINISHED || scanItem.getScanStatus() == ScanStatus.FAILED) {
                log.warn("Duplicate result received for correlationId: {}. " +
                                "Current status: {}. Ignoring.",
                        message.getCorrelationId(), scanItem.getScanStatus());
                return;
            }

            if (message.isSuccess()) {
                log.info("Scan SUCCEEDED. CorrelationId: {}, UserId: {}, Target: {}, Duration: {}ms",
                        message.getCorrelationId(),
                        scanItem.getUserId(),
                        scanItem.getTarget(),
                        message.getExecutionTimeMs());

                scanItem.setScanStatus(ScanStatus.FINISHED);
                scanItem.setScanOutput(message.getScanOutput());
                scanItem.setCompletedAt(message.getCompletedAt());
                scanItem.setExecutionTimeMs(message.getExecutionTimeMs());

                if (message.getScanOutput() != null) {
                    log.debug("Scan output size: {} bytes", message.getScanOutput().length());
                }

            } else {
                log.error("Scan FAILED. CorrelationId: {}, UserId: {}, Error: {}",
                        message.getCorrelationId(),
                        scanItem.getUserId(),
                        message.getErrorMessage());

                scanItem.setScanStatus(ScanStatus.FAILED);
                scanItem.setErrorMessage(message.getErrorMessage());
                scanItem.setCompletedAt(message.getCompletedAt());
            }

            scanItemRepository.save(scanItem);
            log.info("Scan item updated in database. CorrelationId: {}, Status: {}",
                    message.getCorrelationId(), scanItem.getScanStatus());

            if (message.isSuccess() && message.getScanOutput() != null) {
                asyncEnrichmentExecutor.enrich(scanItem.getScanItemId(), message.getScanOutput());
            }

        } catch (Exception e) {
            log.error("Error processing scan result. CorrelationId: {}, Error: {}",
                    message.getCorrelationId(), e.getMessage(), e);

            throw new RuntimeException("Failed to process scan result", e);
        }
    }
}