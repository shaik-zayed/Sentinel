package org.sentinel.scanservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@Slf4j
@Component
public class NvdClient {

    private static final String NVD_BASE_URL = "https://services.nvd.nist.gov/rest/json/cves/2.0";

    private static final int MAX_RESULTS = 5;

    private final RateLimiter rateLimiter;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public NvdClient(
            @Qualifier("nvdRestTemplate") RestTemplate restTemplate,
            ObjectMapper objectMapper,
            RateLimiter rateLimiter) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.rateLimiter = rateLimiter;
    }

    @Retryable(
            retryFor = {ResourceAccessException.class, HttpClientErrorException.TooManyRequests.class},
            noRetryFor = {
                    HttpClientErrorException.BadRequest.class,
                    HttpClientErrorException.Unauthorized.class,
                    HttpClientErrorException.Forbidden.class,
                    HttpClientErrorException.NotFound.class
            },
            maxAttempts = 4,
            backoff = @Backoff(delay = 10_000, multiplier = 2, maxDelay = 60_000)
    )
    public JsonNode fetchCves(String product, String version, String cpe) {
        log.debug("fetchCves called: product='{}', version='{}', cpe='{}'", product, version, cpe);

        // Strategy 1: CPE-based lookup (only when CPE has a real version)
        if (cpe != null && !cpe.isBlank() && isCpeVersioned(cpe)) {
            String cpe23 = convertCpe22to23(cpe);
            if (cpe23 != null) {
                URI cpeUri = UriComponentsBuilder.fromUriString(NVD_BASE_URL)
                        .queryParam("cpeName", cpe23)
                        .build()
                        .encode()
                        .toUri();

                log.info("NVD CPE lookup: {}", cpeUri);

                try {
                    return fetchLatest(cpeUri.toString(), product, version);
                } catch (HttpClientErrorException.NotFound e) {
                    log.warn("NVD CPE lookup returned 404 — falling back to keyword search. " +
                            "product={} version={} cpe={}", product, version, cpe23);
                } catch (HttpClientErrorException.TooManyRequests e) {
                    throw e;
                } catch (HttpClientErrorException e) {
                    log.warn("NVD CPE lookup returned {} — falling back to keyword search. " +
                            "product={} version={} cpe={}", e.getStatusCode(), product, version, cpe23);
                }
            }
        } else if (cpe != null && !cpe.isBlank()) {
            log.debug("CPE has no concrete version — skipping CPE lookup, using keyword search. cpe={}", cpe);
        }

        // Strategy 2: Keyword search
        String cleanVersion = cleanVersionForKeyword(version);
        String keyword = buildKeyword(cpe, product, cleanVersion);

        URI keywordUri = UriComponentsBuilder.fromUriString(NVD_BASE_URL)
                .queryParam("keywordSearch", keyword)
                .build()
                .encode()
                .toUri();

        log.info("NVD keyword lookup: {}", keywordUri);
        return fetchLatest(keywordUri.toString(), product, version);
    }


    @Recover
    public JsonNode recoverFetchCves(ResourceAccessException e,
                                     String product, String version, String cpe) {
        log.error("NVD API permanently failed after retries (network error). " +
                "product={} version={} cpe={}: {}", product, version, cpe, e.getMessage());
        return emptyResult();
    }

    @Recover
    public JsonNode recoverFetchCves(HttpClientErrorException.TooManyRequests e,
                                     String product, String version, String cpe) {
        log.error("NVD API returned 429 after all retries — rate limit exhausted. " +
                "product={} version={} cpe={}: {}", product, version, cpe, e.getMessage());
        return emptyResult();
    }

    @Recover
    public JsonNode recoverFetchCves(HttpClientErrorException e,
                                     String product, String version, String cpe) {
        log.error("NVD API HTTP error — not retrying further. " +
                        "product={} version={} cpe={}: {} {}",
                product, version, cpe, e.getStatusCode(), e.getMessage());
        return emptyResult();
    }

    @Recover
    public JsonNode recoverFetchCves(Exception e,
                                     String product, String version, String cpe) {
        log.error("Unexpected error during CVE fetch. product={} version={} cpe={}: {}",
                product, version, cpe, e.getMessage(), e);
        return emptyResult();
    }

    private JsonNode fetchLatest(String baseUrl, String product, String version) {

        URI probeUri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("resultsPerPage", 1)
                .queryParam("startIndex", 0)
                .build()
                .encode()
                .toUri();

        log.debug("NVD probe request: {}", probeUri);
        JsonNode probePage = parseResponse(
                fetchRaw(probeUri.toString(), product, version), product, version);
        if (probePage == null) return emptyResult();

        int totalResults = probePage.path("totalResults").asInt(0);
        log.info("NVD totalResults={} for product={} version={}", totalResults, product, version);

        if (totalResults == 0) return emptyResult();

        int startIndex = Math.max(0, totalResults - MAX_RESULTS);

        URI fetchUri = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("resultsPerPage", MAX_RESULTS)
                .queryParam("startIndex", startIndex)
                .build()
                .encode()
                .toUri();

        log.debug("NVD fetch request: {}", fetchUri);
        JsonNode page = parseResponse(
                fetchRaw(fetchUri.toString(), product, version), product, version);
        if (page == null) return emptyResult();

        JsonNode vulns = page.path("vulnerabilities");
        ObjectNode result = objectMapper.createObjectNode();
        result.put("totalResults", totalResults);
        result.set("vulnerabilities", vulns.isArray() ? vulns : objectMapper.createArrayNode());
        return result;
    }

    private JsonNode emptyResult() {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("totalResults", 0);
        node.set("vulnerabilities", objectMapper.createArrayNode());
        return node;
    }

    private String fetchRaw(String url, String product, String version) {
        rateLimiter.acquire();
        try {
            return restTemplate.getForObject(url, String.class);
        } catch (HttpClientErrorException e) {
            log.error("NVD API client error for product={} version={}: {} {}",
                    product, version, e.getStatusCode(), e.getMessage());
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("NVD API network error for product={} version={}: {}",
                    product, version, e.getMessage());
            throw e;
        }
    }

    static boolean isCpeVersioned(String cpe) {
        if (cpe == null || cpe.isBlank()) return false;

        if (cpe.startsWith("cpe:2.3:")) {
            String[] parts = cpe.split(":", -1);
            if (parts.length < 6) return false;
            String version = parts[5].trim();
            return !version.isEmpty() && !version.equals("*");
        }

        if (cpe.startsWith("cpe:/")) {
            String body = cpe.substring("cpe:/".length());
            String[] parts = body.split(":", -1);
            if (parts.length < 4) return false;
            String version = parts[3].trim();
            return !version.isEmpty() && !version.equals("*");
        }

        return false;
    }

    static String convertCpe22to23(String cpe) {
        if (cpe == null || cpe.isBlank()) return null;

        if (cpe.startsWith("cpe:2.3:")) return cpe;

        if (!cpe.startsWith("cpe:/")) {
            log.warn("Unrecognised CPE format (neither 2.2 nor 2.3): {}", cpe);
            return null;
        }

        String body = cpe.substring("cpe:/".length());
        String[] parts = body.split(":", -1);

        if (parts.length < 3) return null;

        String[] components = new String[11];
        Arrays.fill(components, "*");

        for (int i = 0; i < Math.min(parts.length, 11); i++) {
            String part = parts[i].trim();
            if (part.isEmpty()) {
                components[i] = "*";
                continue;
            }
            try {
                part = URLDecoder.decode(part, StandardCharsets.UTF_8);
            } catch (IllegalArgumentException e) {
                log.warn("Could not URL-decode CPE 2.2 component '{}', using as-is", part);
            }
            components[i] = part;
        }

        if ("*".equals(components[0]) || "*".equals(components[1]) || "*".equals(components[2])) {
            log.warn("CPE 2.2 missing required fields after conversion: {}", cpe);
            return null;
        }

        String result = "cpe:2.3:" + String.join(":", components);
        log.debug("Converted CPE 2.2 → 2.3: {} → {}", cpe, result);
        return result;
    }

    private String buildKeyword(String cpe, String product, String cleanVersion) {
        if (cpe != null && !cpe.isBlank()) {
            String cpeKeyword = extractKeywordFromCpe(cpe, cleanVersion);
            if (cpeKeyword != null) {
                log.debug("Using CPE-derived keyword: '{}'", cpeKeyword);
                return cpeKeyword;
            }
        }

        String cleanProduct = cleanProductForKeyword(product);
        return cleanProduct + (cleanVersion.isBlank() ? "" : " " + cleanVersion);
    }

    private String extractKeywordFromCpe(String cpe, String cleanVersion) {
        String vendor = null;
        String prod = null;

        if (cpe.startsWith("cpe:2.3:")) {
            String[] parts = cpe.split(":", -1);
            if (parts.length >= 5) {
                vendor = parts[3].trim();
                prod = parts[4].trim();
            }
        } else if (cpe.startsWith("cpe:/")) {
            String body = cpe.substring("cpe:/".length());
            String[] parts = body.split(":", -1);
            if (parts.length >= 3) {
                vendor = parts[1].trim();
                prod = parts[2].trim();
            }
        }

        if (vendor == null || vendor.isEmpty() || vendor.equals("*")) return null;
        if (prod == null || prod.isEmpty() || prod.equals("*")) return null;

        vendor = vendor.replace('_', ' ');
        prod = prod.replace('_', ' ');

        String base = vendor.equalsIgnoreCase(prod)
                ? vendor
                : vendor + " " + prod;

        return base + (cleanVersion.isBlank() ? "" : " " + cleanVersion);
    }

    private String cleanProductForKeyword(String product) {
        if (product == null || product.isBlank()) return "";

        String cleaned = product.contains("/")
                ? product.substring(0, product.indexOf('/')).trim()
                : product.trim();

        String[] knownSuffixes = {"smbd", "ftpd", "httpd", "sshd", "named", "mysqld", "nginx"};
        String lower = cleaned.toLowerCase();
        for (String suffix : knownSuffixes) {
            String pattern = " " + suffix;
            if (lower.endsWith(pattern)) {
                String candidate = cleaned.substring(0, cleaned.length() - pattern.length()).trim();
                if (!candidate.isEmpty()) {
                    cleaned = candidate;
                    break;
                }
            }
        }
        return cleaned;
    }

    private String cleanVersionForKeyword(String version) {
        if (version == null || version.isBlank()) return "";

        String firstToken = version.split("\\s+")[0];

        if (firstToken.contains("-")) {
            String beforeHyphen = firstToken.substring(0, firstToken.indexOf('-'));
            if (!beforeHyphen.isEmpty() && beforeHyphen.matches(".*[0-9].*")) {
                firstToken = beforeHyphen;
            }
        }
        return firstToken;
    }

    private JsonNode parseResponse(String response, String product, String version) {
        if (response == null) return null;
        try {
            return objectMapper.readTree(response);
        } catch (Exception e) {
            log.error("Failed to parse NVD response for product={} version={}: {}",
                    product, version, e.getMessage());
            throw new RuntimeException("NVD response parse failed", e);
        }
    }
}