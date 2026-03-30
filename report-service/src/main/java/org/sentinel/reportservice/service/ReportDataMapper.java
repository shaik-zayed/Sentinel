package org.sentinel.reportservice.service;

import org.sentinel.reportservice.dto.ReportData;
import org.sentinel.reportservice.dto.ReportData.HostInfo;
import org.sentinel.reportservice.dto.ReportData.PortInfo;
import org.sentinel.reportservice.dto.ReportData.ScriptInfo;
import org.sentinel.reportservice.model.*;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Maps a parsed NmapRun into a ReportData object.
 * Centralises all nmap model knowledge in one place so the generators
 * stay clean and format-focused.
 */
@Component
public class ReportDataMapper {

    public ReportData map(UUID scanId, NmapRun run) {
        Host firstHost = (run.hosts() != null && !run.hosts().isEmpty())
                ? run.hosts().get(0) : null;

        return ReportData.builder()
                .scanId(scanId)
                .target(resolveTarget(firstHost))
                .scanStarted(run.startStr())
                .scanFinished(resolveFinished(run))
                .elapsedSeconds(resolveElapsed(run))
                .nmapVersion(run.version())
                .nmapArgs(run.args())
                .hostInfo(buildHostInfo(firstHost, run.runstats()))
                .openPorts(buildPortInfos(firstHost))
                .build();
    }

    // -----------------------------------------------------------------------

    private String resolveTarget(Host host) {
        if (host == null || host.addresses() == null || host.addresses().isEmpty()) return null;
        return host.addresses().get(0).addr();
    }

    private String resolveFinished(NmapRun run) {
        if (run.runstats() == null || run.runstats().finished() == null) return null;
        return run.runstats().finished().timeString();
    }

    private String resolveElapsed(NmapRun run) {
        if (run.runstats() == null || run.runstats().finished() == null) return null;
        return run.runstats().finished().elapsed();
    }

    private HostInfo buildHostInfo(Host host, RunStats stats) {
        HostInfo.HostInfoBuilder b = HostInfo.builder();

        if (stats != null && stats.hosts() != null) {
            b.hostsUp(stats.hosts().up())
                    .hostsDown(stats.hosts().down())
                    .hostsTotal(stats.hosts().total());
        }

        if (host != null) {
            b.state(host.status() != null ? host.status().state().name().toLowerCase() : "unknown")
                    .reason(host.status() != null ? host.status().reason() : null)
                    .addresses(host.addresses() != null
                            ? host.addresses().stream().map(Address::addr).collect(Collectors.toList())
                            : Collections.emptyList())
                    .hostnames(host.hostnames() != null && host.hostnames().hostname() != null
                            ? host.hostnames().hostname().stream()
                            .map(Hostname::name)
                            .filter(n -> n != null && !n.isBlank())
                            .collect(Collectors.toList())
                            : Collections.emptyList());
        }

        return b.build();
    }

    private List<PortInfo> buildPortInfos(Host host) {
        if (host == null || host.ports() == null || host.ports().port() == null) {
            return Collections.emptyList();
        }

        return host.ports().port().stream()
                .filter(p -> p.state() != null && "open".equals(p.state().state()))
                .map(p -> {
                    Service svc = p.service();
                    return PortInfo.builder()
                            .portId(p.portId())
                            .protocol(p.protocol().name().toLowerCase())
                            .state(p.state().state())
                            .serviceName(svc != null ? svc.name() : null)
                            .product(svc != null ? svc.product() : null)
                            .version(svc != null ? svc.version() : null)
                            .extraInfo(svc != null ? svc.extraInfo() : null)
                            .tlsEnabled(svc != null && svc.tunnel() != null)
                            .cpes(svc != null && svc.cpes() != null ? svc.cpes() : Collections.emptyList())
                            .scripts(buildScriptInfos(p.scripts()))
                            .build();
                })
                .collect(Collectors.toList());
    }

    private List<ScriptInfo> buildScriptInfos(List<Script> scripts) {
        if (scripts == null) return Collections.emptyList();
        return scripts.stream()
                .map(s -> ScriptInfo.builder().id(s.id()).output(s.output()).build())
                .collect(Collectors.toList());
    }
}