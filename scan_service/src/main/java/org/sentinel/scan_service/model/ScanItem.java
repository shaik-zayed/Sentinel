package org.sentinel.scan_service.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "scan_item", indexes = {
        @Index(name = "idx_correlation_id", columnList = "correlationId"),
        @Index(name = "idx_user_status", columnList = "userId, scanStatus"),
        @Index(name = "idx_completed_at", columnList = "completedAt")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scanItemId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private UUID scanRequestId;

    @Column(unique = true, length = 36)
    private String correlationId;

    @Column(nullable = false)
    private String target;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "scan_item_commands",
            joinColumns = @JoinColumn(name = "scan_item_id"))
    @Column(name = "command_part")
    private List<String> scanCommand;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScanStatus scanStatus;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    private Instant completedAt;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String scanOutput;

    private Long executionTimeMs;

    @Column(length = 1000)
    private String errorMessage;

    @Version
    private Long version;

    public boolean isCompleted() {
        return scanStatus == ScanStatus.FINISHED;
    }

    public boolean isFailed() {
        return scanStatus == ScanStatus.FAILED;
    }

    public boolean isInProgress() {
        return scanStatus == ScanStatus.QUEUED ||
                scanStatus == ScanStatus.PENDING;
    }

    public long getAgeSec() {
        return java.time.Duration.between(createdAt, Instant.now()).getSeconds();
    }
}