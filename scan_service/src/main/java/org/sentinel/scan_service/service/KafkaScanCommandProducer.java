package org.sentinel.scan_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scan_service.model.ScanCommandMessage;
import org.sentinel.scan_service.model.ScanItem;
import org.sentinel.scan_service.model.ScanStatus;
import org.sentinel.scan_service.repo.ScanItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaScanCommandProducer {

    @Value("${topics.output-name}")
    private String topicName;

    private final KafkaTemplate<String, ScanCommandMessage> kafkaTemplate;
    private final ScanItemRepository scanItemRepository;

    public void pushScanCommand(ScanCommandMessage message) {
        String key = message.getCorrelationId();

        log.debug("Sending scan command to Kafka. Topic: {}, Key: {}, Target: {}",
                topicName, key, message.getTarget());

        CompletableFuture<SendResult<String, ScanCommandMessage>> future =
                kafkaTemplate.send(topicName, key, message);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("CRITICAL: Failed to send scan command to Kafka. " +
                                "CorrelationId: {}, Topic: {}, Error: {}",
                        message.getCorrelationId(), topicName, ex.getMessage(), ex);

                // Update scan status to FAILED
                updateScanStatusToFailed(message.getScanItemId(),
                        "Failed to send to Kafka: " + ex.getMessage());

            } else {
                SendResult<String, ScanCommandMessage> sendResult = result;
                log.info("Scan command sent to Kafka. " +
                                "CorrelationId: {}, Topic: {}, Partition: {}, Offset: {}, UserId: {}",
                        message.getCorrelationId(),
                        topicName,
                        sendResult.getRecordMetadata().partition(),
                        sendResult.getRecordMetadata().offset(),
                        message.getUserId());
            }
        });
    }

    private void updateScanStatusToFailed(UUID scanItemId, String errorMessage) {
        try {
            ScanItem scanItem = scanItemRepository.findById(scanItemId)
                    .orElseThrow(() -> new RuntimeException("ScanItem not found: " + scanItemId));

            scanItem.setScanStatus(ScanStatus.FAILED);
            scanItem.setErrorMessage(errorMessage);
            scanItemRepository.save(scanItem);

            log.info("Updated scan status to FAILED. ScanItemId: {}", scanItemId);

        } catch (Exception e) {
            log.error("Failed to update scan status. ScanItemId: {}", scanItemId, e);
        }
    }
}