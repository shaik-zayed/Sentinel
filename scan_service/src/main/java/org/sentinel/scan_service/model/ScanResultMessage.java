package org.sentinel.scan_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanResultMessage {
    private String correlationId;
    private UUID userId;
    private UUID scanRequestId;
    private UUID scanItemId;
    private String scanOutput;
    private boolean success;
    private String errorMessage;
    private Integer exitCode;
    private Instant completedAt;
    private Long executionTimeMs;
    private String warnings;
}