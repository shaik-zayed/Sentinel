package org.sentinel.reportservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.client.ScanClient;
import org.sentinel.reportservice.dto.ReportData;
import org.sentinel.reportservice.dto.ReportResponse;
import org.sentinel.reportservice.dto.ScanResultResponse;
import org.sentinel.reportservice.exception.ScanNotCompleteException;
import org.sentinel.reportservice.exception.ScanNotFoundException;
import org.sentinel.reportservice.model.ReportFormat;
import org.sentinel.reportservice.model.NmapRun;
import org.sentinel.reportservice.service.generator.DocxReportGenerator;
import org.sentinel.reportservice.service.generator.HtmlReportGenerator;
import org.sentinel.reportservice.service.generator.JsonReportGenerator;
import org.sentinel.reportservice.service.generator.PdfReportGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Orchestrates the full report generation lifecycle:
 * <p>
 * 1. Check MinIO — if the report is already cached, return the URL immediately.
 * 2. Fetch the nmap XML from scan-service via @HttpExchange (one HTTP call).
 * 3. Parse the XML into a NmapRun, map to ReportData.
 * 4. Delegate to the appropriate generator.
 * 5. Upload the result to MinIO.
 * 6. Return a pre-signed download URL.
 * <p>
 * All steps are synchronous. Report generation from XML takes < 1 second
 * for all four formats, making async unnecessary here.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReportOrchestrator {

    private final ScanClient scanClient;
    private final NmapParseService nmapParseService;
    private final ReportDataMapper reportDataMapper;
    private final MinioStorageService minioStorage;
    private final JsonReportGenerator jsonGenerator;
    private final HtmlReportGenerator htmlGenerator;
    private final PdfReportGenerator pdfGenerator;
    private final DocxReportGenerator docxGenerator;

    @Value("${minio.url-expiry-minutes}")
    private int urlExpiryMinutes;

    /**
     * Main entry point.
     * Returns a ReportResponse with a pre-signed download URL.
     *
     * @param scanId the scan to report on
     * @param format requested output format
     * @param userId the authenticated user (forwarded to scan-service)
     */
    public ReportResponse getOrGenerate(UUID scanId, ReportFormat format, String userId) {
        String objectPath = format.objectPath(scanId.toString());

        // ── Step 1: Cache hit ─────────────────────────────────────────────
        if (minioStorage.exists(objectPath)) {
            log.info("Cache hit - returning existing report. scanId={}, format={}", scanId, format);
            return buildResponse(scanId, format, objectPath);
        }

        // ── Step 2: Fetch XML from scan-service ───────────────────────────
        log.info("Cache miss - generating report. scanId={}, format={}", scanId, format);
        ScanResultResponse scanResult = fetchScanResult(scanId, userId);

        // ── Step 3: Parse + Map ───────────────────────────────────────────
        NmapRun nmapRun;
        try {
            nmapRun = nmapParseService.parse(scanResult.getScanOutput());
        } catch (Exception e) {
            log.error("Failed to parse nmap XML for scanId={}: {}", scanId, e.getMessage());
            throw new RuntimeException("Failed to parse scan output: " + e.getMessage(), e);
        }
        ReportData reportData = reportDataMapper.map(scanId, nmapRun);

        // ── Step 4: Generate ──────────────────────────────────────────────
        byte[] content = generate(format, reportData);

        // ── Step 5: Upload to MinIO ───────────────────────────────────────
        minioStorage.upload(objectPath, content, format.contentType());
        log.info("Report stored in MinIO. scanId={}, format={}, size={} bytes",
                scanId, format, content.length);

        // ── Step 6: Return URL ────────────────────────────────────────────
        return buildResponse(scanId, format, objectPath);
    }

    /**
     * Returns which formats are already cached in MinIO for a given scan.
     */
    public ReportResponse getCachedFormats(UUID scanId) {
        Map<String, Boolean> cached = new LinkedHashMap<>();
        for (ReportFormat fmt : ReportFormat.values()) {
            cached.put(fmt.name().toLowerCase(),
                    minioStorage.exists(fmt.objectPath(scanId.toString())));
        }

        return ReportResponse.builder()
                .scanId(scanId)
                .cachedFormats(cached)
                .build();
    }

    // -----------------------------------------------------------------------
    // Private helpers
    // -----------------------------------------------------------------------

    private ScanResultResponse fetchScanResult(UUID scanId, String userId) {
        try {
            ScanResultResponse result = scanClient.getScanResult(scanId, userId);

            // scan-service returns scanOutput only when FINISHED
            // (its getScanResult throws BadRequestException if not complete)
            // This guard is a safety net in case that contract ever changes
            if (result.getScanOutput() == null || result.getScanOutput().isBlank()) {
                throw new ScanNotCompleteException(
                        "Scan output is empty. Status: " + result.getStatus());
            }

            return result;

        } catch (HttpClientErrorException.NotFound e) {
            throw new ScanNotFoundException("Scan not found: " + scanId);

        } catch (HttpClientErrorException.Forbidden e) {
            // Re-throw as-is - the controller will return 403
            throw e;

        } catch (HttpClientErrorException.BadRequest e) {
            // scan-service throws 400 when the scan is not yet FINISHED
            throw new ScanNotCompleteException(
                    "Scan is not complete yet. " + e.getResponseBodyAsString());

        } catch (ScanNotFoundException | ScanNotCompleteException e) {
            throw e;

        } catch (Exception e) {
            log.error("Failed to fetch scan result from scan-service. scanId={}, error={}",
                    scanId, e.getMessage());
            throw new RuntimeException(
                    "Failed to retrieve scan data: " + e.getMessage(), e);
        }
    }

    private byte[] generate(ReportFormat format, ReportData data) {
        return switch (format) {
            case PDF -> pdfGenerator.generate(data);
            case HTML -> htmlGenerator.generate(data);
            case JSON -> jsonGenerator.generate(data);
            case DOCX -> docxGenerator.generate(data);
        };
    }

    private ReportResponse buildResponse(UUID scanId, ReportFormat format, String objectPath) {
        String url = minioStorage.generatePresignedUrl(objectPath);
        return ReportResponse.builder()
                .scanId(scanId)
                .format(format.name().toLowerCase())
                .status("READY")
                .downloadUrl(url)
                .expiresIn(urlExpiryMinutes + " minutes")
                .contentType(format.contentType())
                .build();
    }
}