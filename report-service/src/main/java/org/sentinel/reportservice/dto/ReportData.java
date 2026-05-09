package org.sentinel.reportservice.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

/**
 * Intermediate representation of a parsed nmap scan result.
 * All four generators (PDF, HTML, JSON, DOCX) consume this object.
 * Built once from NmapRun - generators never touch XML directly.
 */
@Data
@Builder
public class ReportData {

    private UUID scanId;
    private String target;
    private String scanStarted;
    private String scanFinished;
    private String elapsedSeconds;
    private String nmapVersion;
    private String nmapArgs;

    private HostInfo hostInfo;
    private List<PortInfo> openPorts;
    private List<CveFinding> cveFindings;

    @Data
    @Builder
    public static class HostInfo {
        private String state;           // up / down / unknown
        private String reason;
        private int hostsUp;
        private int hostsDown;
        private int hostsTotal;
        private List<String> addresses;
        private List<String> hostnames;
    }

    @Data
    @Builder
    public static class PortInfo {
        private int portId;
        private String protocol;        // tcp / udp
        private String state;           // open / closed / filtered
        private String serviceName;
        private String product;
        private String version;
        private String extraInfo;
        private boolean tlsEnabled;
        private List<String> cpes;
        private List<ScriptInfo> scripts;
    }

    @Data
    @Builder
    public static class ScriptInfo {
        private String id;
        private String output;
    }
}