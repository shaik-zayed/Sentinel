package org.sentinel.reportservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.dto.ReportResponse;
import org.sentinel.reportservice.model.ReportFormat;
import org.sentinel.reportservice.service.ReportOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportGenerationController {

    private final ReportOrchestrator orchestrator;

    /**
     * Generate (or return cached) report in the requested format.
     * <p>
     * Example:
     * POST /api/v1/report/3fa85f64-5717-4562-b3fc-2c963f66afa6?format=pdf
     * Authorization: Bearer <token>
     * <p>
     * Response:
     * 200 OK
     * {
     * "scanId": "3fa85f64-...",
     * "format": "pdf",
     * "status": "READY",
     * "downloadUrl": "http://localhost:9000/sentinel-reports/reports/3fa85f64-.../report.pdf?X-Amz-...",
     * "expiresIn": "60 minutes",
     * "contentType": "application/pdf"
     * }
     */
    @PostMapping("/{scanId}")
    public ResponseEntity<ReportResponse> generateReport(
            @PathVariable UUID scanId,
            @RequestParam String format,
            @RequestHeader("X-User-Id") String userId) {

        ReportFormat reportFormat = parseFormat(format);

        log.info("Report requested. scanId={}, format={}, userId={}", scanId, reportFormat, userId);

        ReportResponse response = orchestrator.getOrGenerate(scanId, reportFormat, userId);

        log.info("Report ready. scanId={}, format={}, cached={}", scanId, reportFormat,
                response.getDownloadUrl() != null);

        return ResponseEntity.ok(response);
    }

    /**
     * List which formats are already cached in MinIO for this scan.
     * <p>
     * Example:
     * GET /api/v1/report/3fa85f64-.../formats
     * <p>
     * Response:
     * 200 OK
     * {
     * "scanId": "3fa85f64-...",
     * "cachedFormats": {
     * "pdf":  true,
     * "html": false,
     * "json": true,
     * "docx": false
     * }
     * }
     */
    @GetMapping("/{scanId}/formats")
    public ResponseEntity<ReportResponse> getCachedFormats(
            @PathVariable UUID scanId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(orchestrator.getCachedFormats(scanId));
    }

    @GetMapping("/{scanId}/download")
    public ResponseEntity<byte[]> downloadReport(
            @PathVariable UUID scanId,
            @RequestParam String format,
            @RequestHeader("X-User-Id") String userId) {

        ReportFormat reportFormat = parseFormat(format);

        log.info("Download requested. scanId={}, format={}, userId={}", scanId, reportFormat, userId);

        byte[] content = orchestrator.getReportBytes(scanId, reportFormat, userId);

        String filename = "report-" + scanId + "." + format.toLowerCase();

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(reportFormat.contentType()))
                .body(content);
    }
    // ----------------------------------------------------------------------------------------

    private ReportFormat parseFormat(String format) {
        try {
            return ReportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported format '" + format + "'. Supported: pdf, html, json, docx");
        }
    }
}