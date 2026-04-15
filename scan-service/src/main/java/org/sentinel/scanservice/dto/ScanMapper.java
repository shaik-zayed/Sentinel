package org.sentinel.scanservice.dto;

import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.model.ScanStatus;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ScanMapper {
    public ScanSubmissionResponse toSubmissionResponse(UUID scanItemId) {
        return ScanSubmissionResponse.builder()
                .scanId(scanItemId)
                .status("ACCEPTED")
                .message("Scan queued successfully")
                .statusUrl("/api/v1/scan/" + scanItemId)
                .build();
    }

    public ScanResponse toDetailResponse(ScanItem scan) {
        return ScanResponse.builder()
                .scanId(scan.getScanItemId())
                .status(scan.getScanStatus().name())
                .target(scan.getTarget())
                .scanCommand(scan.getScanCommand())
                .createdAt(scan.getCreatedAt())
                .updatedAt(scan.getUpdatedAt())
                .completedAt(scan.getCompletedAt())
                .executionTimeMs(scan.getExecutionTimeMs())
                .errorMessage(scan.getErrorMessage())
                .scanOutput(scan.getScanStatus() == ScanStatus.FINISHED
                        ? scan.getScanOutput()
                        : null)
                .build();
    }

    public ScanResponse toStatusResponse(ScanItem scan) {
        return ScanResponse.builder()
                .scanId(scan.getScanItemId())
                .status(scan.getScanStatus().name())
                .target(scan.getTarget())
                .createdAt(scan.getCreatedAt())
                .completedAt(scan.getCompletedAt())
                .executionTimeMs(scan.getExecutionTimeMs())
                // Don't include scanCommand, scanOutput
                .build();
    }

    public ScanResponse toResultResponse(ScanItem scan) {
        return ScanResponse.builder()
                .scanId(scan.getScanItemId())
                .target(scan.getTarget())
                .scanOutput(scan.getScanOutput())
                .completedAt(scan.getCompletedAt())
                .executionTimeMs(scan.getExecutionTimeMs())
                .build();
    }

    public ScanResponse toSummaryResponse(ScanItem scan) {
        return ScanResponse.builder()
                .scanId(scan.getScanItemId())
                .status(scan.getScanStatus().name())
                .target(scan.getTarget())
                .createdAt(scan.getCreatedAt())
                .completedAt(scan.getCompletedAt())
                .build();
    }
}
