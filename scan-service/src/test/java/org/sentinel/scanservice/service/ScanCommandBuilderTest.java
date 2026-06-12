package org.sentinel.scanservice.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.sentinel.scanservice.model.ScanRequest;
import org.sentinel.scanservice.model.enums.PortMode;
import org.sentinel.scanservice.model.enums.Protocol;
import org.sentinel.scanservice.model.enums.ScanMode;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ScanCommandBuilder")
class ScanCommandBuilderTest {

    private ScanCommandBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new ScanCommandBuilder();
    }

    // -------------------------------------------------------------------------
    // Protocol
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("protocol flags")
    class ScanProtocol {

        @Test
        void tcpProtocolUsesSynScan() {
            ScanRequest req = minimal("example.com");
            req.setProtocol(Protocol.TCP);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd)
                    .contains("-sS")
                    .doesNotContain("-sU");
        }

        @Test
        void udpScanWhenProtocolIsUDP() {
            ScanRequest req = minimal("example.com");
            req.setProtocol(Protocol.UDP);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd)
                    .contains("-sU")
                    .doesNotContain("-sS");
        }

        @Test
        void nullProtocolDefaultsToTcp() {
            ScanRequest req = minimal("example.com");
            req.setProtocol(null);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).contains("-sS");
        }
    }

    // -------------------------------------------------------------------------
    // Service version detection
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("service version detection")
    class ServiceVersion {

        @Test
        void versionFlagsAddedWhenEnabled() {
            ScanRequest req = minimal("example.com");
            req.setDetectServiceVersion(true);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).contains("-sV", "--version-intensity", "5");
        }

        @Test
        void versionFlagsAbsentWhenDisabled() {
            ScanRequest req = minimal("example.com");
            req.setDetectServiceVersion(false);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).isNotEmpty().doesNotContain("-sV", "--version-intensity");
        }
    }

    // -------------------------------------------------------------------------
    // OS detection
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("OS detection")
    class OsDetection {

        @Test
        void osFlagsAddedWhenEnabled() {
            ScanRequest req = minimal("example.com");
            req.setDetectOs(true);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).contains("-O", "--osscan-limit");
        }

        @Test
        void osFlagsAbsentWhenDisabled() {
            ScanRequest req = minimal("example.com");
            req.setDetectOs(false);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).isNotEmpty().doesNotContain("-O");
        }
    }

    // -------------------------------------------------------------------------
    // Port selection
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("port selection")
    class PortSelection {

        @Test
        void nullPortModeAddsNoPortFlags() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(null);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).doesNotContain("--top-ports", "-p");
        }

        @Test
        void top100PortsWhenCommonModeAndTop100Value() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.COMMON);
            req.setPortValue("top-100");

            List<String> cmd = builder.buildCommand(req);

            int idx = cmd.indexOf("--top-ports");
            assertThat(idx).isNotNegative();
            assertThat(cmd.get(idx + 1)).isEqualTo("100");
        }

        @Test
        void top1000PortsWhenCommonModeAndTop1000Value() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.COMMON);
            req.setPortValue("top-1000");

            List<String> cmd = builder.buildCommand(req);

            int idx = cmd.indexOf("--top-ports");
            assertThat(idx).isNotNegative();
            assertThat(cmd.get(idx + 1)).isEqualTo("1000");
        }

        @Test
        void specificPortsWhenListMode() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.LIST);
            req.setPortValue("80,443,8080");

            List<String> cmd = builder.buildCommand(req);

            int idx = cmd.indexOf("-p");
            assertThat(idx).isNotNegative();
            assertThat(cmd.get(idx + 1)).isEqualTo("80,443,8080");
        }

        @Test
        void portRangesAllowedInListMode() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.LIST);
            req.setPortValue("1-1024,8080,9000-9100");

            List<String> cmd = builder.buildCommand(req);

            int idx = cmd.indexOf("-p");
            assertThat(idx).isNotNegative();
            assertThat(cmd.get(idx + 1)).isEqualTo("1-1024,8080,9000-9100");
        }

        @Test
        void nonNumericCharsStrippedFromPortList() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.LIST);
            req.setPortValue("80abc,443");

            assertThatThrownBy(() -> builder.buildCommand(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Invalid port spec '80abc'. Use individual ports (80,443), ranges (9000-10000), or a mix.");
        }

        @Test
        void emptyPortValueInListModeThrows() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.LIST);
            req.setPortValue("");

            assertThatThrownBy(() -> builder.buildCommand(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("PortValue is required for LIST mode");
        }

        @Test
        void nullPortValueInListModeThrows() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.LIST);
            req.setPortValue(null);

            assertThatThrownBy(() -> builder.buildCommand(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void purelyNonNumericPortValueThrowsAfterSanitization() {
            ScanRequest req = minimal("example.com");
            req.setPortMode(PortMode.LIST);
            req.setPortValue("abc;xyz");

            assertThatThrownBy(() -> builder.buildCommand(req))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // -------------------------------------------------------------------------
    // Scan mode (timing)
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("scan mode timing")
    class ScanModeTest {

        @Test
        void deepModeSetsT4AndAggressive() {
            ScanRequest req = minimal("example.com");
            req.setScanMode(ScanMode.DEEP);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).contains("-T4", "-A");
        }

        @Test
        void lightModeSetsT3() {
            ScanRequest req = minimal("example.com");
            req.setScanMode(ScanMode.LIGHT);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd)
                    .contains("-T3")
                    .doesNotContain("-A");
        }

        @Test
        void nullScanModeDefaultsToLight() {
            ScanRequest req = minimal("example.com");
            req.setScanMode(null);

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).contains("-T3");
        }

        @Test
        void deepModeRejectsUdp() {
            ScanRequest req = minimal("example.com");
            req.setScanMode(ScanMode.DEEP);
            req.setProtocol(Protocol.UDP);

            assertThatThrownBy(() -> builder.buildCommand(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(
                            "Deep scan mode (which includes OS detection) is not supported with UDP scans.");
        }
    }

    // -------------------------------------------------------------------------
    // Output and target always appended last
    // -------------------------------------------------------------------------

    @Nested
    @DisplayName("output format and target positioning")
    class OutputAndTarget {

        @Test
        void xmlOutputFlagAlwaysPresent() {
            ScanRequest req = minimal("8.8.8.8");

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).contains("-oX", "-");
        }

        @Test
        void targetIsLastElement() {
            ScanRequest req = minimal("scanme.nmap.org");

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd.get(cmd.size() - 1)).isEqualTo("scanme.nmap.org");
        }

        @Test
        void commandIsNeverEmpty() {
            ScanRequest req = minimal("example.com");

            List<String> cmd = builder.buildCommand(req);

            assertThat(cmd).isNotEmpty();
        }
    }

    // -------------------------------------------------------------------------
    // Full command snapshot — deep scan, service version, OS, port list
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("deep scan with all options produces complete command")
    void fullDeepScanCommand() {
        ScanRequest req = new ScanRequest();
        req.setTarget("scanme.nmap.org");
        req.setProtocol(Protocol.TCP);
        req.setScanMode(ScanMode.DEEP);
        req.setDetectServiceVersion(true);
        req.setDetectOs(true);
        req.setPortMode(PortMode.LIST);
        req.setPortValue("80,443,22");

        List<String> cmd = builder.buildCommand(req);

        assertThat(cmd)
                .contains("-sS", "-T4", "-A", "-p", "-oX");
        assertThat(cmd.get(cmd.size() - 1)).isEqualTo("scanme.nmap.org");
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ScanRequest minimal(String target) {
        ScanRequest req = new ScanRequest();
        req.setTarget(target);
        req.setScanMode(ScanMode.LIGHT);
        req.setProtocol(Protocol.TCP);
        req.setDetectServiceVersion(false);
        req.setDetectOs(false);
        return req;
    }
}