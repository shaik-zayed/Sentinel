package org.sentinel.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * Mirrors scan-service's ScanResponse DTO.
 * Only the fields report-service actually needs are declared here.
 *
 * @JsonIgnoreProperties ensures new fields added to scan-service
 * never break deserialization here.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ScanResultResponse {
    private UUID scanId;
    private String status;
    private String target;
    private String scanOutput;      // raw nmap XML - only present when status = FINISHED
    private Instant completedAt;
    private Long executionTimeMs;
    private String errorMessage;
}