package org.sentinel.nmapservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.nmapservice.model.ScanResultMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaNmapScanResultProducer {

    @Value("${topics.output-name}")
    private String topicName;

    private final KafkaTemplate<String, ScanResultMessage> kafkaTemplate;

    public void pushScanResult(ScanResultMessage result) {
        String key = result.getCorrelationId();

        log.debug("Sending scan result to Kafka. Topic: {}, Key: {}, Success: {}",
                topicName, key, result.isSuccess());

        CompletableFuture<SendResult<String, ScanResultMessage>> future =
                kafkaTemplate.send(topicName, key, result);

        future.whenComplete((sendResult, ex) -> {
            if (ex != null) {
                log.error("CRITICAL: Failed to send scan result to Kafka. " + "CorrelationId: {}, UserId: {}. Result data may be LOST! Error: {}",
                        result.getCorrelationId(),
                        result.getUserId(),
                        ex.getMessage(),
                        ex);
            } else {
                log.info("Scan result sent to Kafka. " + "CorrelationId: {}, Success: {}, Topic: {}, Partition: {}, Offset: {}, UserId: {}",
                        result.getCorrelationId(),
                        result.isSuccess(),
                        topicName,
                        sendResult.getRecordMetadata().partition(),
                        sendResult.getRecordMetadata().offset(),
                        result.getUserId());
            }
        });
    }


    public void pushScanResultSync(ScanResultMessage result) {
        String key = result.getCorrelationId();

        log.debug("Sending scan result SYNCHRONOUSLY. CorrelationId: {}", key);

        try {
            SendResult<String, ScanResultMessage> sendResult =
                    kafkaTemplate.send(topicName, key, result)
                            .get(10, TimeUnit.SECONDS);

            log.info("Scan result sent (sync). CorrelationId: {}, Partition: {}, Offset: {}",
                    result.getCorrelationId(),
                    sendResult.getRecordMetadata().partition(),
                    sendResult.getRecordMetadata().offset());

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to send scan result to Kafka. CorrelationId: " + key, e);
        }
    }
}