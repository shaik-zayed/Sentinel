package org.sentinel.nmap_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.nmap_service.model.ScanCommandMessage;
import org.sentinel.nmap_service.model.ScanResultMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Service
@RequiredArgsConstructor
@Slf4j
public class KafkaScanCommandConsumer {

    private final DockerService dockerService;
    private final KafkaNmapScanResultProducer resultProducer;
    private final ExecutorService scanExecutor;

    @Value("${nmap.image}")
    private String imageName;

    @KafkaListener(
            topics = "${topics.input-name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeScanCommand(
            @Payload ScanCommandMessage message,
            Acknowledgment ack,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {

        log.info("Received scan command. CorrelationId: {}, UserId: {}, Target: {}, Partition: {}, Offset: {}",
                message.getCorrelationId(),
                message.getUserId(),
                message.getTarget(),
                partition,
                offset);

        CompletableFuture.runAsync(() -> processMessage(message, ack), scanExecutor);
    }

    private void processMessage(ScanCommandMessage message, Acknowledgment ack) {
        Instant startTime = Instant.now();
        String containerName = "nmap-scan-" +
                message.getCorrelationId().substring(0, 8) + "-" + System.nanoTime();

        try {
            log.info("Starting nmap scan. Container: {}, Command: {}, User: {}",
                    containerName, message.getCommand(), message.getUserId());

            String scanOutput = dockerService.startContainer(
                    imageName,
                    containerName,
                    message.getCommand()
            );

            Instant endTime = Instant.now();
            long executionMs = Duration.between(startTime, endTime).toMillis();

            log.info("Scan completed successfully. CorrelationId: {}, Duration: {}ms, Output size: {} bytes",
                    message.getCorrelationId(), executionMs, scanOutput.length());

            ScanResultMessage result = ScanResultMessage.builder()
                    .correlationId(message.getCorrelationId())
                    .userId(message.getUserId())
                    .scanRequestId(message.getScanRequestId())
                    .scanItemId(message.getScanItemId())
                    .scanOutput(scanOutput)
                    .success(true)
                    .exitCode(0)
                    .completedAt(endTime)
                    .executionTimeMs(executionMs)
                    .build();

            resultProducer.pushScanResult(result);

            ack.acknowledge();

            log.info("Scan success + offset committed. CorrelationId: {}", message.getCorrelationId());

        } catch (Exception e) {
            Instant errorTime = Instant.now();
            long executionMs = Duration.between(startTime, errorTime).toMillis();

            log.error("Scan execution FAILED. CorrelationId: {}, Container: {}, Error: {}",
                    message.getCorrelationId(), containerName, e.getMessage(), e);

            ScanResultMessage errorResult = ScanResultMessage.builder()
                    .correlationId(message.getCorrelationId())
                    .userId(message.getUserId())
                    .scanRequestId(message.getScanRequestId())
                    .scanItemId(message.getScanItemId())
                    .success(false)
                    .errorMessage(e.getMessage())
                    .exitCode(-1)
                    .completedAt(errorTime)
                    .executionTimeMs(executionMs)
                    .build();

            resultProducer.pushScanResult(errorResult);

            log.warn("Offset NOT acknowledged (will retry). CorrelationId: {}",
                    message.getCorrelationId());
        }
    }
}