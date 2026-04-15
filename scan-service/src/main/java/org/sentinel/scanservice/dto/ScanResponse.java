package org.sentinel.scanservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ScanResponse {
    // Always included
    private UUID scanId;
    private String status;
    private String target;
    private Instant createdAt;

    // Optional - included based on context
    private List<String> scanCommand;
    private Instant updatedAt;
    private Instant completedAt;
    private Long executionTimeMs;
    private String errorMessage;
    private String scanOutput;
}