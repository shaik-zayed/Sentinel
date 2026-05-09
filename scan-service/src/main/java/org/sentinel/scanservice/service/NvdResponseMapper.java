package org.sentinel.scanservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.Finding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
public class NvdResponseMapper {

    /**
     * Maps raw NVD API JSON response into a list of Finding entities.
     *
     * @param root       raw NVD response JsonNode — may be null if NVD call failed
     * @param scanItemId the scan this finding belongs to
     * @param port       port number
     * @param protocol   tcp/udp
     * @param serviceName service name from Nmap
     * @param product    product name from Nmap
     * @param version    version string from Nmap
     * @param cpe        CPE string from Nmap, may be null
     * @return list of Finding entities, empty if response is null or has no vulnerabilities
     */
    public List<Finding> map(JsonNode root, UUID scanItemId,
                             int port, String protocol, String serviceName,
                             String product, String version, String cpe) {

        List<Finding> findings = new ArrayList<>();

        if (root == null || root.isMissingNode()) {
            log.warn("Null or missing NVD response for product={} version={} — skipping",
                    product, version);
            return findings;
        }

        JsonNode vulnerabilities = root.path("vulnerabilities");

        if (vulnerabilities.isMissingNode() || !vulnerabilities.isArray()) {
            log.debug("No vulnerabilities array in NVD response for product={} version={}",
                    product, version);
            return findings;
        }

        for (JsonNode vuln : vulnerabilities) {
            try {
                JsonNode cveNode = vuln.path("cve");
                String cveId = cveNode.path("id").asText(null);

                if (cveId == null || cveId.isBlank()) {
                    log.debug("Skipping CVE entry with missing ID");
                    continue;
                }

                String description = extractDescription(cveNode);
                CvssData cvssData = extractCvss(cveNode);

                findings.add(Finding.builder()
                        .scanItemId(scanItemId)
                        .port(port)
                        .protocol(protocol)
                        .serviceName(serviceName)
                        .product(product)
                        .version(version)
                        .cpe(cpe)
                        .cveId(cveId)
                        .cvssScore(cvssData.score())
                        .severity(cvssData.severity())
                        .description(description)
                        .build());

            } catch (Exception e) {
                log.warn("Failed to map CVE entry for product={} version={}: {}",
                        product, version, e.getMessage());
                // Skip this entry, continue with the rest
            }
        }

        log.debug("Mapped {} findings for product={} version={}", findings.size(), product, version);
        return findings;
    }

    private String extractDescription(JsonNode cveNode) {
        JsonNode descriptions = cveNode.path("descriptions");
        if (!descriptions.isArray()) return null;

        // Prefer English description
        for (JsonNode desc : descriptions) {
            if ("en".equals(desc.path("lang").asText())) {
                String value = desc.path("value").asText(null);
                if (value != null && !value.isBlank()) {
                    // Truncate to fit the TEXT column cleanly
                    return value.length() > 2000 ? value.substring(0, 2000) : value;
                }
            }
        }
        return null;
    }

    private CvssData extractCvss(JsonNode cveNode) {
        JsonNode metrics = cveNode.path("metrics");

        // Try CVSS v3.1 first (most current)
        if (metrics.has("cvssMetricV31")) {
            JsonNode cvss = metrics.path("cvssMetricV31").path(0).path("cvssData");
            return new CvssData(
                    cvss.path("baseScore").asDouble(0.0),
                    cvss.path("baseSeverity").asText("UNKNOWN")
            );
        }

        // Fall back to CVSS v3.0
        if (metrics.has("cvssMetricV30")) {
            JsonNode cvss = metrics.path("cvssMetricV30").path(0).path("cvssData");
            return new CvssData(
                    cvss.path("baseScore").asDouble(0.0),
                    cvss.path("baseSeverity").asText("UNKNOWN")
            );
        }

        // Fall back to CVSS v2
        if (metrics.has("cvssMetricV2")) {
            JsonNode metric = metrics.path("cvssMetricV2").path(0);
            JsonNode cvss = metric.path("cvssData");
            return new CvssData(
                    cvss.path("baseScore").asDouble(0.0),
                    metric.path("baseSeverity").asText("UNKNOWN") // v2 severity is on metric, not cvssData
            );
        }

        return new CvssData(0.0, "UNKNOWN");
    }

    private record CvssData(double score, String severity) {}
}