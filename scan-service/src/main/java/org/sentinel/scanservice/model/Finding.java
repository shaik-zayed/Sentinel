package org.sentinel.scanservice.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "findings", indexes = {
        @Index(name = "idx_findings_scan_item_id", columnList = "scanItemId"),
        @Index(name = "idx_findings_severity", columnList = "severity")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Finding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID findingId;

    @Column(nullable = false)
    private UUID scanItemId;

    @Column(nullable = false)
    private int port;

    @Column(nullable = false, length = 10)
    private String protocol;

    @Column(length = 100)
    private String serviceName;

    @Column(length = 200)
    private String product;

    @Column(length = 100)
    private String version;

    @Column(length = 200)
    private String cpe;

    @Column(length = 50)
    private String cveId;

    private Double cvssScore;

    @Column(length = 20)
    private String severity;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}