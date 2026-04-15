package org.sentinel.scanservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ScanSubmissionResponse {
    private UUID scanId;
    private String status;
    private String message;
    private String statusUrl;
}