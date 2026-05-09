package org.sentinel.scanservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for NvdClient.
 * <p>
 * fetchCves / fetchLatest are tested via mocked RestTemplate responses.
 * isCpeVersioned and convertCpe22to23 are static package-visible methods
 * tested directly — they cover the full range of real-world Nmap-detected
 * service CPEs across Linux, Windows, macOS, and network equipment.
 */
@ExtendWith(MockitoExtension.class)
class NvdClientTest {

    @Mock
    private RestTemplate restTemplate;

    private ObjectMapper objectMapper;
    private RateLimiter rateLimiter;
    private NvdClient nvdClient;

    // ── NVD response builders ──────────────────────────────────────────────────

    /**
     * Builds a minimal NVD API JSON response with the given totalResults
     * and a vulnerabilities array containing that many placeholder entries.
     */
    private String nvdResponse(int totalResults) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"totalResults\":").append(totalResults).append(",\"vulnerabilities\":[");
        for (int i = 0; i < totalResults; i++) {
            if (i > 0) sb.append(",");
            sb.append("{\"cve\":{\"id\":\"CVE-2024-").append(String.format("%04d", i + 1))
                    .append("\",\"published\":\"2024-01-").append(String.format("%02d", i + 1))
                    .append("T00:00:00.000\"}}");
        }
        sb.append("]}");
        return sb.toString();
    }

    /**
     * Probe response: always totalResults only, 1 entry (the probe result itself).
     * The probe entry is irrelevant — only totalResults matters.
     */
    private String probeResponse(int totalResults) {
        return "{\"totalResults\":" + totalResults + ",\"vulnerabilities\":[" +
                "{\"cve\":{\"id\":\"CVE-2024-0001\",\"published\":\"2024-01-01T00:00:00.000\"}}" +
                "]}";
    }

    /**
     * Empty NVD response — no CVEs found.
     */
    private String emptyNvdResponse() {
        return "{\"totalResults\":0,\"vulnerabilities\":[]}";
    }

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Use a fast rate limiter in tests — no need to actually throttle
        rateLimiter = RateLimiter.create(1000.0);
        nvdClient = new NvdClient(restTemplate, objectMapper, rateLimiter);
    }

    // ==========================================================================
    // fetchCves — two-request strategy (probe + fetch)
    // ==========================================================================

    @Nested
    class FetchLatestStrategy {

        @Test
        void whenTotalResultsGreaterThanMaxResults_shouldJumpToTail() {
            // 20 CVEs exist — startIndex for fetch should be 20 - 5 = 15
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(20))   // probe
                    .thenReturn(nvdResponse(5));      // fetch from startIndex=15

            JsonNode result = nvdClient.fetchCves("openssl", "1.1.1", null);

            assertThat(result.path("totalResults").asInt()).isEqualTo(20);
            assertThat(result.path("vulnerabilities").isArray()).isTrue();
            assertThat(result.path("vulnerabilities").size()).isEqualTo(5);

            // Verify exactly 2 requests were made
            verify(restTemplate, times(2)).getForObject(anyString(), eq(String.class));
        }

        @Test
        void whenTotalResultsLessThanMaxResults_startIndexShouldBeZero() {
            // Only 3 CVEs — startIndex should be max(0, 3-5) = 0
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(3))
                    .thenReturn(nvdResponse(3));

            JsonNode result = nvdClient.fetchCves("somesoftware", "1.0", null);

            assertThat(result.path("totalResults").asInt()).isEqualTo(3);
            assertThat(result.path("vulnerabilities").size()).isEqualTo(3);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            // Second request (fetch) must have startIndex=0
            String fetchUrl = urlCaptor.getAllValues().get(1);
            assertThat(fetchUrl).contains("startIndex=0");
        }

        @Test
        void whenTotalResultsIsExactlyMaxResults_startIndexShouldBeZero() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(5))
                    .thenReturn(nvdResponse(5));

            JsonNode result = nvdClient.fetchCves("nginx", "1.25.0", null);

            assertThat(result.path("totalResults").asInt()).isEqualTo(5);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));
            assertThat(urlCaptor.getAllValues().get(1)).contains("startIndex=0");
        }

        @Test
        void whenNoResultsFound_shouldReturnEmptyResult() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(emptyNvdResponse());

            JsonNode result = nvdClient.fetchCves("unknownproduct", "9.9.9", null);

            assertThat(result.path("totalResults").asInt()).isEqualTo(0);
            assertThat(result.path("vulnerabilities").size()).isEqualTo(0);

            // Only probe request should be made — no fetch when totalResults=0
            verify(restTemplate, times(1)).getForObject(anyString(), eq(String.class));
        }

        @Test
        void probeRequestShouldUseResultsPerPageOne() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(10))
                    .thenReturn(nvdResponse(5));

            nvdClient.fetchCves("apache", "2.4.51", null);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            String probeUrl = urlCaptor.getAllValues().get(0);
            assertThat(probeUrl).contains("resultsPerPage=1");
            assertThat(probeUrl).contains("startIndex=0");
        }

        @Test
        void fetchRequestShouldUseMaxResultsFive() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(100))
                    .thenReturn(nvdResponse(5));

            nvdClient.fetchCves("apache", "2.4.51", null);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            String fetchUrl = urlCaptor.getAllValues().get(1);
            assertThat(fetchUrl).contains("resultsPerPage=5");
            assertThat(fetchUrl).contains("startIndex=95");
        }
    }

    // ==========================================================================
    // fetchCves — CPE strategy vs keyword fallback
    // ==========================================================================

    @Nested
    class FetchCvesStrategy {

        @Test
        void whenVersionedCpe23Given_shouldUseCpeLookupFirst() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(10))
                    .thenReturn(nvdResponse(5));

            nvdClient.fetchCves("OpenSSL", "3.0.2", "cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*");

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            // Both probe and fetch URLs should use cpeName parameter
            assertThat(urlCaptor.getAllValues().get(0)).contains("cpeName=");
            assertThat(urlCaptor.getAllValues().get(1)).contains("cpeName=");
        }

        @Test
        void whenVersionedCpe22Given_shouldConvertAndUseCpeLookup() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(8))
                    .thenReturn(nvdResponse(5));

            nvdClient.fetchCves("OpenSSH", "8.2", "cpe:/a:openbsd:openssh:8.2");

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            assertThat(urlCaptor.getAllValues().get(0)).contains("cpeName=");
            // CPE must be converted to 2.3 format
            assertThat(urlCaptor.getAllValues().get(0)).contains("cpe:2.3:a:openbsd:openssh:8.2");
        }

        @Test
        void whenUnversionedCpe23Given_shouldSkipCpeLookupAndUseKeyword() {
            // CPE with wildcard version — should fall straight to keyword search
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(5))
                    .thenReturn(nvdResponse(5));

            nvdClient.fetchCves("Apache httpd", "2.4.51",
                    "cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*");

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            assertThat(urlCaptor.getAllValues().get(0)).contains("keywordSearch=");
        }

        @Test
        void whenCpeLookupReturns404_shouldFallBackToKeywordSearch() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    // CPE probe throws 404
                    .thenThrow(HttpClientErrorException.create(
                            HttpStatus.NOT_FOUND, "Not Found", null, null, null))
                    // keyword probe and fetch succeed
                    .thenReturn(probeResponse(3))
                    .thenReturn(nvdResponse(3));

            JsonNode result = nvdClient.fetchCves("OpenSSL", "3.0.2",
                    "cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*");

            // Should still return results from keyword search
            assertThat(result.path("totalResults").asInt()).isEqualTo(3);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(3)).getForObject(urlCaptor.capture(), eq(String.class));

            // Third request should be keyword-based
            assertThat(urlCaptor.getAllValues().get(2)).contains("keywordSearch=");
        }

        @Test
        void whenNoCpeGiven_shouldUseKeywordSearch() {
            when(restTemplate.getForObject(anyString(), eq(String.class)))
                    .thenReturn(probeResponse(7))
                    .thenReturn(nvdResponse(5));

            nvdClient.fetchCves("nginx", "1.25.3", null);

            ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
            verify(restTemplate, times(2)).getForObject(urlCaptor.capture(), eq(String.class));

            assertThat(urlCaptor.getAllValues().get(0)).contains("keywordSearch=");
        }

//        @Test
//        void whenNetworkErrorOccurs_shouldReturnEmptyResult() {
//            when(restTemplate.getForObject(anyString(), eq(String.class)))
//                    .thenThrow(new ResourceAccessException("Connection refused"));
//
//            JsonNode result = nvdClient.fetchCves("apache", "2.4.51", null);
//
//            // Should not throw — should return empty result from @Recover
//            assertThat(result).isNotNull();
//            assertThat(result.path("totalResults").asInt()).isEqualTo(0);
//        }
//
//        @Test
//        void whenRateLimitExhausted_shouldReturnEmptyResult() {
//            when(restTemplate.getForObject(anyString(), eq(String.class)))
//                    .thenThrow(HttpClientErrorException.create(
//                            HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", null, null, null));
//
//            JsonNode result = nvdClient.fetchCves("apache", "2.4.51", null);
//
//            assertThat(result).isNotNull();
//            assertThat(result.path("totalResults").asInt()).isEqualTo(0);
//        }
    }

    // ==========================================================================
    // isCpeVersioned — all CPE formats and edge cases
    // ==========================================================================

    @Nested
    class IsCpeVersioned {

        // ── CPE 2.3 — versioned ───────────────────────────────────────────────

        @Test
        void cpe23_linuxApache_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:apache:http_server:2.4.51:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxOpenSSH_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:openbsd:openssh:8.9:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxOpenSSL_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxNginx_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:nginx:nginx:1.25.3:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxMySQL_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:mysql:mysql:8.0.32:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxVsftpd_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:vsftpd_project:vsftpd:3.0.5:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxPostfix_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:postfix:postfix:3.7.0:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxBind_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:isc:bind:9.18.4:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_linuxSamba_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:samba:samba:4.17.2:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_windowsIIS_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:microsoft:iis:10.0:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_windowsSQLServer_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:microsoft:sql_server:2019:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_windowsRDP_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:microsoft:remote_desktop_services:10.0:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_windowsExchange_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:microsoft:exchange_server:2019:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_networkCisco_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:o:cisco:ios:15.2:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_networkJuniper_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:o:juniper:junos:22.1:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_macOSApache_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:apache:http_server:2.4.54:*:*:*:*:macos:*:*")).isTrue();
        }

        @Test
        void cpe23_dockerDaemon_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:docker:docker:24.0.5:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_redis_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:redis:redis:7.2.0:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_elasticSearch_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:elastic:elasticsearch:8.9.0:*:*:*:*:*:*:*")).isTrue();
        }

        @Test
        void cpe23_tomcat_versioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:apache:tomcat:10.1.13:*:*:*:*:*:*:*")).isTrue();
        }

        // ── CPE 2.3 — unversioned (wildcard) ─────────────────────────────────

        @Test
        void cpe23_wildcardVersion_notVersioned() {
            assertThat(NvdClient.isCpeVersioned(
                    "cpe:2.3:a:apache:http_server:*:*:*:*:*:*:*:*")).isFalse();
        }

        @Test
        void cpe23_tooShort_notVersioned() {
            assertThat(NvdClient.isCpeVersioned("cpe:2.3:a:apache")).isFalse();
        }

        // ── CPE 2.2 — versioned ───────────────────────────────────────────────

        @Test
        void cpe22_apache_versioned() {
            assertThat(NvdClient.isCpeVersioned("cpe:/a:apache:http_server:2.4.51")).isTrue();
        }

        @Test
        void cpe22_openssh_versioned() {
            assertThat(NvdClient.isCpeVersioned("cpe:/a:openbsd:openssh:8.9")).isTrue();
        }

        @Test
        void cpe22_mysql_versioned() {
            assertThat(NvdClient.isCpeVersioned("cpe:/a:mysql:mysql:8.0.32")).isTrue();
        }

        @Test
        void cpe22_iis_versioned() {
            assertThat(NvdClient.isCpeVersioned("cpe:/a:microsoft:iis:10.0")).isTrue();
        }

        @Test
        void cpe22_wildcardVersion_notVersioned() {
            assertThat(NvdClient.isCpeVersioned("cpe:/a:apache:http_server:*")).isFalse();
        }

        @Test
        void cpe22_tooShort_notVersioned() {
            // Only part:vendor:product — no version field
            assertThat(NvdClient.isCpeVersioned("cpe:/a:apache:http_server")).isFalse();
        }

        // ── Null / blank / garbage inputs ─────────────────────────────────────

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "not-a-cpe", "cpe:1.0:a:x:y:1.0"})
        void invalidInputs_notVersioned(String cpe) {
            assertThat(NvdClient.isCpeVersioned(cpe)).isFalse();
        }
    }

    // ==========================================================================
    // convertCpe22to23 — all real-world service formats
    // ==========================================================================

    @Nested
    class ConvertCpe22to23 {

        // ── Already CPE 2.3 — pass through unchanged ──────────────────────────

        @Test
        void alreadyCpe23_returnedAsIs() {
            String cpe = "cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*";
            assertThat(NvdClient.convertCpe22to23(cpe)).isEqualTo(cpe);
        }

        // ── Standard Linux services ───────────────────────────────────────────

        @Test
        void apache_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:apache:http_server:2.4.51");
            assertThat(result).isEqualTo("cpe:2.3:a:apache:http_server:2.4.51:*:*:*:*:*:*:*");
        }

        @Test
        void openssh_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:openbsd:openssh:8.9");
            assertThat(result).isEqualTo("cpe:2.3:a:openbsd:openssh:8.9:*:*:*:*:*:*:*");
        }

        @Test
        void openssl_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:openssl:openssl:3.0.2");
            assertThat(result).isEqualTo("cpe:2.3:a:openssl:openssl:3.0.2:*:*:*:*:*:*:*");
        }

        @Test
        void nginx_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:nginx:nginx:1.25.3");
            assertThat(result).isEqualTo("cpe:2.3:a:nginx:nginx:1.25.3:*:*:*:*:*:*:*");
        }

        @Test
        void vsftpd_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:vsftpd_project:vsftpd:3.0.5");
            assertThat(result).isEqualTo("cpe:2.3:a:vsftpd_project:vsftpd:3.0.5:*:*:*:*:*:*:*");
        }

        @Test
        void mysql_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:mysql:mysql:8.0.32");
            assertThat(result).isEqualTo("cpe:2.3:a:mysql:mysql:8.0.32:*:*:*:*:*:*:*");
        }

        @Test
        void samba_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:samba:samba:4.17.2");
            assertThat(result).isEqualTo("cpe:2.3:a:samba:samba:4.17.2:*:*:*:*:*:*:*");
        }

        @Test
        void postfix_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:postfix:postfix:3.7.0");
            assertThat(result).isEqualTo("cpe:2.3:a:postfix:postfix:3.7.0:*:*:*:*:*:*:*");
        }

        @Test
        void bind_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:isc:bind:9.18.4");
            assertThat(result).isEqualTo("cpe:2.3:a:isc:bind:9.18.4:*:*:*:*:*:*:*");
        }

        @Test
        void redis_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:redis:redis:7.2.0");
            assertThat(result).isEqualTo("cpe:2.3:a:redis:redis:7.2.0:*:*:*:*:*:*:*");
        }

        @Test
        void tomcat_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:apache:tomcat:10.1.13");
            assertThat(result).isEqualTo("cpe:2.3:a:apache:tomcat:10.1.13:*:*:*:*:*:*:*");
        }

        // ── Windows services ──────────────────────────────────────────────────

        @Test
        void windowsIIS_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:microsoft:iis:10.0");
            assertThat(result).isEqualTo("cpe:2.3:a:microsoft:iis:10.0:*:*:*:*:*:*:*");
        }

        @Test
        void windowsSQLServer_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:microsoft:sql_server:2019");
            assertThat(result).isEqualTo("cpe:2.3:a:microsoft:sql_server:2019:*:*:*:*:*:*:*");
        }

        @Test
        void windowsExchange_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:microsoft:exchange_server:2019");
            assertThat(result).isEqualTo("cpe:2.3:a:microsoft:exchange_server:2019:*:*:*:*:*:*:*");
        }

        @Test
        void windowsRDP_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23(
                    "cpe:/a:microsoft:remote_desktop_services:10.0");
            assertThat(result).isEqualTo(
                    "cpe:2.3:a:microsoft:remote_desktop_services:10.0:*:*:*:*:*:*:*");
        }

        // ── Network equipment ─────────────────────────────────────────────────

        @Test
        void ciscoIOS_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/o:cisco:ios:15.2");
            assertThat(result).isEqualTo("cpe:2.3:o:cisco:ios:15.2:*:*:*:*:*:*:*");
        }

        @Test
        void juniperJunos_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/o:juniper:junos:22.1");
            assertThat(result).isEqualTo("cpe:2.3:o:juniper:junos:22.1:*:*:*:*:*:*:*");
        }

        @Test
        void fortinetFortiOS_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/o:fortinet:fortios:7.2.4");
            assertThat(result).isEqualTo("cpe:2.3:o:fortinet:fortios:7.2.4:*:*:*:*:*:*:*");
        }

        // ── URL-encoded components (Fix 3) ────────────────────────────────────

        @Test
        void urlEncodedExclamation_decodedCorrectly() {
            // %21 is URL-encoding for '!'
            String result = NvdClient.convertCpe22to23("cpe:/a:vendor:product%211:1.0");
            assertThat(result).isNotNull();
            assertThat(result).contains("product!1");
        }

        @Test
        void urlEncodedSpace_decodedCorrectly() {
            // %20 is URL-encoding for ' '
            String result = NvdClient.convertCpe22to23("cpe:/a:microsoft:sql%20server:2019");
            assertThat(result).isNotNull();
            assertThat(result).contains("sql server");
        }

        @Test
        void urlEncodedPlus_decodedCorrectly() {
            // %2B is URL-encoding for '+'
            String result = NvdClient.convertCpe22to23("cpe:/a:vendor:product%2Bextra:1.0");
            assertThat(result).isNotNull();
            assertThat(result).contains("product+extra");
        }

        // ── Output format validation ───────────────────────────────────────────

        @Test
        void convertedCpe_hasExactly13ColonSeparatedSegments() {
            // cpe:2.3 = 1, then 11 attribute components = 12 more colons = 13 segments
            String result = NvdClient.convertCpe22to23("cpe:/a:apache:http_server:2.4.51");
            assertThat(result).isNotNull();
            assertThat(result.split(":", -1)).hasSize(13);
        }

        @Test
        void convertedCpe_startsWithCpe23Prefix() {
            String result = NvdClient.convertCpe22to23("cpe:/a:nginx:nginx:1.25.3");
            assertThat(result).startsWith("cpe:2.3:");
        }

        @Test
        void convertedCpe_missingFieldsFilledWithWildcard() {
            // Only part:vendor:product:version — update onwards should be *
            String result = NvdClient.convertCpe22to23("cpe:/a:nginx:nginx:1.25.3");
            assertThat(result).isNotNull();
            String[] parts = result.split(":", -1);
            // parts[0]="cpe", [1]="2.3", [2]=part, [3]=vendor, [4]=product, [5]=version
            // [6]=update onwards should all be *
            for (int i = 6; i < parts.length; i++) {
                assertThat(parts[i]).isEqualTo("*");
            }
        }

        // ── Invalid / null inputs ─────────────────────────────────────────────

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void nullOrBlank_returnsNull(String cpe) {
            assertThat(NvdClient.convertCpe22to23(cpe)).isNull();
        }

        @Test
        void unrecognisedFormat_returnsNull() {
            assertThat(NvdClient.convertCpe22to23("cpe:1.0:a:vendor:product:1.0")).isNull();
        }

        @Test
        void cpe22TooShort_returnsNull() {
            // Less than 3 components after cpe:/
            assertThat(NvdClient.convertCpe22to23("cpe:/a:vendor")).isNull();
        }

        @Test
        void cpe22MissingVendor_returnsNull() {
            // part is * — required field missing
            assertThat(NvdClient.convertCpe22to23("cpe:/:vendor:product:1.0")).isNull();
        }

        // ── CPE 2.2 with full fields including update/edition ─────────────────

        @Test
        void cpe22WithUpdateField_convertedCorrectly() {
            String result = NvdClient.convertCpe22to23("cpe:/a:apache:http_server:2.4.51:sp1");
            assertThat(result).isEqualTo("cpe:2.3:a:apache:http_server:2.4.51:sp1:*:*:*:*:*:*");
        }

        @ParameterizedTest(name = "[{index}] CPE 2.2 {0} -> CPE 2.3 starts with cpe:2.3:")
        @CsvSource({
                "cpe:/a:docker:docker:24.0.5,        cpe:2.3:a:docker:docker:24.0.5:*:*:*:*:*:*:*",
                "cpe:/a:elastic:elasticsearch:8.9.0, cpe:2.3:a:elastic:elasticsearch:8.9.0:*:*:*:*:*:*:*",
                "cpe:/a:mongodb:mongodb:6.0.8,       cpe:2.3:a:mongodb:mongodb:6.0.8:*:*:*:*:*:*:*",
                "cpe:/a:postgresql:postgresql:15.4,  cpe:2.3:a:postgresql:postgresql:15.4:*:*:*:*:*:*:*",
                "cpe:/a:haproxy:haproxy:2.8.3,       cpe:2.3:a:haproxy:haproxy:2.8.3:*:*:*:*:*:*:*",
                "cpe:/a:squid-cache:squid:6.3,       cpe:2.3:a:squid-cache:squid:6.3:*:*:*:*:*:*:*",
                "cpe:/a:proftpd:proftpd:1.3.8,       cpe:2.3:a:proftpd:proftpd:1.3.8:*:*:*:*:*:*:*",
                "cpe:/a:dovecot:dovecot:2.3.21,      cpe:2.3:a:dovecot:dovecot:2.3.21:*:*:*:*:*:*:*",
                "cpe:/a:exim:exim:4.96,              cpe:2.3:a:exim:exim:4.96:*:*:*:*:*:*:*",
                "cpe:/o:paloaltonetworks:pan-os:11.0, cpe:2.3:o:paloaltonetworks:pan-os:11.0:*:*:*:*:*:*:*"
        })
        void parameterised_cpe22ToCpe23Conversions(String input, String expected) {
            assertThat(NvdClient.convertCpe22to23(input.trim())).isEqualTo(expected.trim());
        }
    }
}