package org.sentinel.scanservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.ScanCommandMessage;
import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.model.ScanStatus;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
                updateScanStatusToFailed(message.getScanItemId(),
                        "Failed to send to Kafka: " + ex.getMessage());
            } else {
                log.info("Scan command sent to Kafka. " +
                                "CorrelationId: {}, Topic: {}, Partition: {}, Offset: {}, UserId: {}",
                        message.getCorrelationId(),
                        topicName,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset(),
                        message.getUserId());
            }
        });
    }

    // Synchronous: blocks until Kafka broker acknowledges the message.
    // Throws on failure so the publisher can retry via the outbox.
    // timeoutSeconds: how long to wait for broker ack before throwing.
    public void pushScanCommandSync(String topic, String key,
                                    ScanCommandMessage message,
                                    int timeoutSeconds) throws Exception {
        log.debug("Sending scan command SYNC to Kafka. Topic: {}, Key: {}, Target: {}",
                topic, key, message.getTarget());

        SendResult<String, ScanCommandMessage> result =
                kafkaTemplate.send(topic, key, message)
                        .get(timeoutSeconds, TimeUnit.SECONDS);

        log.info("Scan command SYNC sent. CorrelationId: {}, Partition: {}, Offset: {}",
                message.getCorrelationId(),
                result.getRecordMetadata().partition(),
                result.getRecordMetadata().offset());
    }

    // ── EXISTING PRIVATE METHOD — unchanged ───────────────────────────────────
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