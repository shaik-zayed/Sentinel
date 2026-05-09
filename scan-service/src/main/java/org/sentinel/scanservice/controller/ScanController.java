package org.sentinel.scanservice.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sentinel.scanservice.dto.ScanResponse;
import org.sentinel.scanservice.dto.ScanSubmissionResponse;
import org.sentinel.scanservice.model.FindingsResponse;
import org.sentinel.scanservice.model.ScanRequest;
import org.sentinel.scanservice.service.FindingService;
import org.sentinel.scanservice.service.ScanService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/scan")
@RequiredArgsConstructor
@Validated
public class ScanController {

    private final ScanService scanService;
    private final FindingService findingService;

    @PostMapping("/submit")
    public ResponseEntity<ScanSubmissionResponse> submitScan(
            @Valid @RequestBody ScanRequest request,
            @RequestHeader(value = "X-User-Id") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        ScanSubmissionResponse response = scanService.submitScan(request, userId);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .header("Location", response.getStatusUrl())
                .body(response);
    }

    @GetMapping("/{scanId}")
    public ResponseEntity<ScanResponse> getScan(
            @PathVariable UUID scanId,
            @RequestHeader(value = "X-User-Id") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        ScanResponse response = scanService.getScanDetails(scanId, userId);

        HttpStatus status = response.getStatus().equals("FINISHED") ||
                response.getStatus().equals("FAILED")
                ? HttpStatus.OK : HttpStatus.ACCEPTED;

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{scanId}/status")
    public ResponseEntity<ScanResponse> getScanStatus(
            @PathVariable UUID scanId,
            @RequestHeader(value = "X-User-Id") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        ScanResponse response = scanService.getScanStatus(scanId, userId);

        HttpStatus status = response.getStatus().equals("FINISHED") ||
                response.getStatus().equals("FAILED")
                ? HttpStatus.OK : HttpStatus.ACCEPTED;

        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{scanId}/result")
    public ResponseEntity<ScanResponse> getScanResult(
            @PathVariable UUID scanId,
            @RequestHeader(value = "X-User-Id") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        ScanResponse response = scanService.getScanResult(scanId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{scanId}/findings")
    public ResponseEntity<FindingsResponse> getScanFindings(
            @PathVariable UUID scanId,
            @RequestHeader(value = "X-User-Id") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        FindingsResponse response = findingService.getFindings(scanId, userId);
        boolean isSettled = switch (response.enrichmentStatus()) {
            case "COMPLETED", "NOT_APPLICABLE", "FAILED", "PARTIAL" -> true;
            default -> false;
        };

        HttpStatus status = isSettled ? HttpStatus.OK : HttpStatus.ACCEPTED;
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/list")
    public ResponseEntity<List<ScanResponse>> listUserScans(
            @RequestHeader(value = "X-User-Id") String userIdHeader,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int page) {

        UUID userId = UUID.fromString(userIdHeader);
        List<ScanResponse> response = scanService.getUserScans(userId, limit, page);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{scanId}")
    public ResponseEntity<Void> deleteScan(
            @PathVariable UUID scanId,
            @RequestHeader(value = "X-User-Id") String userIdHeader) {

        UUID userId = UUID.fromString(userIdHeader);
        scanService.deleteScan(scanId, userId);
        return ResponseEntity.noContent().build();
    }
}