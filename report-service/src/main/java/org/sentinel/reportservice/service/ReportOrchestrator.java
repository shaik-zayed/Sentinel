package org.sentinel.reportservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.client.ScanClient;
import org.sentinel.reportservice.dto.CveFindingsResponse;
import org.sentinel.reportservice.dto.ReportData;
import org.sentinel.reportservice.dto.ReportResponse;
import org.sentinel.reportservice.dto.ScanResultResponse;
import org.sentinel.reportservice.exception.EnrichmentPendingException;
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
 * 1. Fetch enrichment status – if not settled, refuse to serve/generate.
 * 2. Check MinIO cache – only if enrichment was settled when originally created.
 * 3. Fetch nmap XML from scan-service.
 * 4. Parse + map into ReportData (attach CVE findings).
 * 5. Delegate to the appropriate report generator.
 * 6. Upload result to MinIO.
 * 7. Return download URL.
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

    @Value("${report.download-base-url:http://localhost:7777}")
    private String downloadBaseUrl;

    /**
     * Main entry point.
     * Throws {@link EnrichmentPendingException} if enrichment is not yet settled,
     * giving the client a 202 Accepted and the current enrichment status.
     */
    public ReportResponse getOrGenerate(UUID scanId, ReportFormat format, String userId) {
        String objectPath = format.objectPath(scanId.toString());

        // ── Step 1: Check enrichment status BEFORE the cache hit ───────────
        CveFindingsResponse findingsResponse = fetchFindings(scanId, userId);
        String enrichmentStatus = findingsResponse != null
                ? findingsResponse.getEnrichmentStatus() : null;

        if (!isEnrichmentSettled(enrichmentStatus)) {
            throw new EnrichmentPendingException(enrichmentStatus);
        }

        // ── Step 2: Cache hit (valid only because enrichment was settled) ─
        if (minioStorage.exists(objectPath)) {
            log.info("Cache hit - returning existing report. scanId={}, format={}", scanId, format);
            return buildResponse(scanId, format, objectPath);
        }

        // ── Step 3: Fetch nmap XML ───────────────────────────────────────
        log.info("Cache miss - generating report. scanId={}, format={}", scanId, format);
        ScanResultResponse scanResult = fetchScanResult(scanId, userId);

        // ── Step 4: Parse + Map ──────────────────────────────────────────
        NmapRun nmapRun;
        try {
            nmapRun = nmapParseService.parse(scanResult.getScanOutput());
        } catch (Exception e) {
            log.error("Failed to parse nmap XML for scanId={}: {}", scanId, e.getMessage());
            throw new RuntimeException("Failed to parse scan output: " + e.getMessage(), e);
        }
        ReportData reportData = reportDataMapper.map(scanId, nmapRun);

        // Attach CVE findings – already fetched above, no extra HTTP call
        if (findingsResponse != null
                && findingsResponse.getFindings() != null
                && !findingsResponse.getFindings().isEmpty()) {
            reportData.setCveFindings(findingsResponse.getFindings());
            log.info("Attached {} CVE findings to report for scanId={}",
                    findingsResponse.getFindings().size(), scanId);
        }

        // ── Step 5: Generate ─────────────────────────────────────────────
        byte[] content = generate(format, reportData);

        // ── Step 6: Upload to MinIO ─────────────────────────────────────
        minioStorage.upload(objectPath, content, format.contentType());
        log.info("Report stored in MinIO. scanId={}, format={}, size={} bytes",
                scanId, format, content.length);

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

    public byte[] getReportBytes(UUID scanId, ReportFormat format, String userId) {
        String objectPath = format.objectPath(scanId.toString());
        if (!minioStorage.exists(objectPath)) {
            // getOrGenerate will throw if enrichment not settled
            getOrGenerate(scanId, format, userId);
        }
        return minioStorage.download(objectPath);
    }

    // -----------------------------------------------------------------------

    private ScanResultResponse fetchScanResult(UUID scanId, String userId) {
        try {
            ScanResultResponse result = scanClient.getScanResult(scanId, userId);
            if (result.getScanOutput() == null || result.getScanOutput().isBlank()) {
                throw new ScanNotCompleteException(
                        "Scan output is empty. Status: " + result.getStatus());
            }
            return result;
        } catch (HttpClientErrorException.NotFound e) {
            throw new ScanNotFoundException("Scan not found: " + scanId);
        } catch (HttpClientErrorException.Forbidden e) {
            throw e;
        } catch (HttpClientErrorException.BadRequest e) {
            throw new ScanNotCompleteException(
                    "Scan is not complete yet. " + e.getResponseBodyAsString());
        } catch (ScanNotFoundException | ScanNotCompleteException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to fetch scan result from scan-service. scanId={}, error={}",
                    scanId, e.getMessage());
            throw new RuntimeException("Failed to retrieve scan data: " + e.getMessage(), e);
        }
    }

    /**
     * Fetches CVE findings from scan-service.
     * Auth failures (403) are propagated; transient errors are swallowed.
     */
    private CveFindingsResponse fetchFindings(UUID scanId, String userId) {
        try {
            return scanClient.getFindings(scanId, userId);
        } catch (HttpClientErrorException.Forbidden e) {
            throw e; // Do not swallow auth failures
        } catch (HttpClientErrorException.NotFound e) {
            throw new ScanNotFoundException("Scan not found: " + scanId);
        } catch (Exception e) {
            log.warn("Could not fetch CVE findings for scanId={}: {}. " +
                    "Treating as enrichment not applicable.", scanId, e.getMessage());
            return null;
        }
    }

    private boolean isEnrichmentSettled(String status) {
        if (status == null) return true; // couldn't fetch – allow generation without CVEs
        return switch (status) {
            case "COMPLETED", "PARTIAL", "FAILED", "NOT_APPLICABLE" -> true;
            default -> false; // PENDING, IN_PROGRESS
        };
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
        String downloadUrl = downloadBaseUrl
                + "/api/v1/report/" + scanId
                + "/download?format=" + format.name().toLowerCase();
        return ReportResponse.builder()
                .scanId(scanId)
                .format(format.name().toLowerCase())
                .status("READY")
                .downloadUrl(downloadUrl)
                .expiresIn("session")
                .contentType(format.contentType())
                .build();
    }
}