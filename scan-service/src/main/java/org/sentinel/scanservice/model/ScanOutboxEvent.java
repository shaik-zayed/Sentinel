package org.sentinel.scanservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "scan_outbox_event",
        indexes = {
                // The publisher polls this constantly — must be fast
                @Index(name = "idx_outbox_published_created", columnList = "published, createdAt")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanOutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // One outbox row per scan submission — unique prevents double-insert on race
    @Column(nullable = false, unique = true)
    private UUID scanItemId;

    @Column(nullable = false, length = 255)
    private String topic;

    // Kafka message key = correlationId — determines partition routing
    @Column(nullable = false, length = 255)
    private String messageKey;

    // Full JSON of ScanCommandMessage — serialized at write time, deserialized at publish time
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    // Set when successfully published — used for cleanup
    private Instant publishedAt;

    @Column(nullable = false)
    @Builder.Default
    private int retryCount = 0;

    @Column(length = 500)
    private String lastError;

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}