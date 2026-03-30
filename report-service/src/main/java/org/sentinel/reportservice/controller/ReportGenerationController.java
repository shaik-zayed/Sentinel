package org.sentinel.reportservice.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.dto.ReportResponse;
import org.sentinel.reportservice.model.ReportFormat;
import org.sentinel.reportservice.service.ReportOrchestrator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Two endpoints:
 *
 *   POST /api/v1/report/{scanId}?format=pdf|html|json|docx
 *     → Generates or returns cached report.
 *     → Returns { status: "READY", downloadUrl: "..." } immediately.
 *     → No polling needed.
 *
 *   GET  /api/v1/report/{scanId}/formats
 *     → Shows which formats are already cached in MinIO.
 *     → Useful for a frontend to show "Download PDF / HTML / ..." buttons.
 *
 * The X-User-Id header is injected by api-gateway's JwtValidationFilter
 * from the JWT. It is forwarded to scan-service to authorise the XML fetch.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/report")
@RequiredArgsConstructor
public class ReportGenerationController {

    private final ReportOrchestrator orchestrator;

    /**
     * Generate (or return cached) report in the requested format.
     *
     * Example:
     *   POST /api/v1/report/3fa85f64-5717-4562-b3fc-2c963f66afa6?format=pdf
     *   Authorization: Bearer <token>
     *
     * Response:
     *   200 OK
     *   {
     *     "scanId": "3fa85f64-...",
     *     "format": "pdf",
     *     "status": "READY",
     *     "downloadUrl": "http://localhost:9000/sentinel-reports/reports/3fa85f64-.../report.pdf?X-Amz-...",
     *     "expiresIn": "60 minutes",
     *     "contentType": "application/pdf"
     *   }
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
     *
     * Example:
     *   GET /api/v1/report/3fa85f64-.../formats
     *
     * Response:
     *   200 OK
     *   {
     *     "scanId": "3fa85f64-...",
     *     "cachedFormats": {
     *       "pdf":  true,
     *       "html": false,
     *       "json": true,
     *       "docx": false
     *     }
     *   }
     */
    @GetMapping("/{scanId}/formats")
    public ResponseEntity<ReportResponse> getCachedFormats(
            @PathVariable UUID scanId,
            @RequestHeader("X-User-Id") String userId) {

        return ResponseEntity.ok(orchestrator.getCachedFormats(scanId));
    }

    // -----------------------------------------------------------------------

    private ReportFormat parseFormat(String format) {
        try {
            return ReportFormat.valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Unsupported format '" + format + "'. Supported: pdf, html, json, docx");
        }
    }
}