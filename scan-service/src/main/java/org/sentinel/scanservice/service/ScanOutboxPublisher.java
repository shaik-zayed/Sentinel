package org.sentinel.scanservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.ScanCommandMessage;
import org.sentinel.scanservice.model.ScanOutboxEvent;
import org.sentinel.scanservice.model.ScanStatus;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.sentinel.scanservice.repo.ScanOutboxEventRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScanOutboxPublisher {

    private final ScanOutboxEventRepository outboxRepository;
    private final ScanItemRepository scanItemRepository;
    private final KafkaScanCommandProducer kafkaProducer;

    // Must match JavaTimeModule used in ScanService — Instant serialized as ISO-8601
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    private static final int MAX_RETRIES = 5;
    private static final int KAFKA_TIMEOUT_SECONDS = 10;

    /**
     * Polls every 10 seconds for unpublished outbox events.
     *
     * fixedDelay = wait 2s AFTER the previous execution finishes.
     * This naturally handles slow batches — if processing takes 5s, the next
     * cycle starts 2s after it finishes, not 2s after it started.
     *
     * There is NO @Transactional here. Each event is handled individually
     * below. This prevents one bad event from rolling back the entire batch.
     *
     * The @Scheduled method is called by Spring's scheduler thread directly —
     * NOT through a Spring proxy — so any @Transactional here would be bypassed
     * anyway. Correctness is achieved by keeping each save() call atomic at the
     * SimpleJpaRepository level (each save() uses its own transaction by default).
     */
    @Scheduled(fixedDelay = 10000)
    public void publishPendingEvents() {
        List<ScanOutboxEvent> pending =
                outboxRepository.findTop100ByPublishedFalseOrderByCreatedAtAsc();

        if (pending.isEmpty()) {
            return;
        }

        log.debug("Outbox: processing {} pending events", pending.size());

        for (ScanOutboxEvent event : pending) {

            // Skip dead-letter events — they need manual intervention
            if (event.getRetryCount() >= MAX_RETRIES) {
                log.error("Outbox: DEAD LETTER — event {} for scanItem {} has {} retries. " +
                                "Manual investigation required. LastError: {}",
                        event.getId(), event.getScanItemId(),
                        event.getRetryCount(), event.getLastError());
                continue;
            }

            try {
                // 1. Deserialize stored JSON payload back to message object.
                //    Must use the same ObjectMapper (with JavaTimeModule) that serialized it.
                ScanCommandMessage message = objectMapper.readValue(
                        event.getPayload(), ScanCommandMessage.class);

                // 2. Synchronous Kafka send — blocks until broker acknowledges.
                //    If this throws, we do NOT mark the event as published.
                //    The exception is caught below and retry count is incremented.
                kafkaProducer.pushScanCommandSync(
                        event.getTopic(),
                        event.getMessageKey(),
                        message,
                        KAFKA_TIMEOUT_SECONDS
                );

                // 3. Kafka ack received. Mark outbox event as published.
                //    Each save() here runs in its own auto-committed transaction
                //    (Spring Data SimpleJpaRepository default behaviour).
                event.setPublished(true);
                event.setPublishedAt(Instant.now());    // ← was missing in previous version
                outboxRepository.save(event);

                // 4. Transition scan status: ACCEPTED → QUEUED.
                //    Uses a conditional UPDATE — safe against concurrent publishers
                //    (if two instances both try this, only one succeeds; second is a no-op).
                //    This is exactly the same pattern as ScanItemRepository.updateEnrichmentStatus.
                int updated = scanItemRepository.transitionScanStatus(
                        event.getScanItemId(),
                        ScanStatus.ACCEPTED,
                        ScanStatus.QUEUED
                );

                if (updated == 0) {
                    // Status was already changed (cancelled, or another publisher got here first)
                    log.warn("Outbox: scan {} status was not ACCEPTED when trying to set QUEUED " +
                                    "(cancelled or duplicate publisher). Kafka message was sent — nmap-service " +
                                    "will receive it and the result will be discarded by the result consumer.",
                            event.getScanItemId());
                } else {
                    log.info("Outbox: published scan job. ScanItemId: {}, CorrelationId: {}",
                            event.getScanItemId(), event.getMessageKey());
                }

            } catch (Exception e) {
                // Kafka send failed OR deserialization failed OR DB save failed.
                // Increment retry count and try again next cycle.
                log.error("Outbox: failed to publish event {} for scan {} (attempt {}): {}",
                        event.getId(), event.getScanItemId(),
                        event.getRetryCount() + 1, e.getMessage());

                event.setRetryCount(event.getRetryCount() + 1);
                event.setLastError(e.getMessage() != null
                        ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 500))
                        : "unknown");
                outboxRepository.save(event);
                // Do NOT re-throw — continue processing the next event in the batch
            }
        }
    }

    /**
     * Cleans up old published events daily at 3 AM.
     * Keeps the outbox table from growing unboundedly.
     * Only deletes rows where published=true AND publishedAt is set
     * (rows where publishedAt is null are stuck events, not yet published).
     */
    @Scheduled(cron = "0 0 3 * * *")
    public void cleanupPublishedEvents() {
        Instant cutoff = Instant.now().minus(7, ChronoUnit.DAYS);
        int deleted = outboxRepository.deletePublishedBefore(cutoff);
        if (deleted > 0) {
            log.info("Outbox cleanup: deleted {} published events older than 7 days", deleted);
        }
    }

    /**
     * Hourly warning for events stuck at max retries.
     * These will never be retried automatically — operator action needed.
     */
    @Scheduled(fixedDelay = 3_600_000)
    public void warnOnDeadLetters() {
        long deadCount = outboxRepository.countByPublishedFalse();
        // Count all unpublished — most will be transient, dead letters are a subset
        // A proper dead-letter alert would filter by retryCount >= MAX_RETRIES,
        // but that requires an additional repository query. This is a quick indicator.
        if (deadCount > 0) {
            log.warn("Outbox: {} events pending publish (includes retrying and dead-letter events). " +
                    "Check logs for DEAD LETTER entries.", deadCount);
        }
    }
}