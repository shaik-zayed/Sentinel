package org.sentinel.reportservice.service.generator;

import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.dto.ReportData;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Builds a self-contained dark-themed HTML report from ReportData.
 * No external template engines — pure string construction.
 * All CSS is inlined so the file is portable.
 */
@Slf4j
@Component
public class HtmlReportGenerator {

    public byte[] generate(ReportData data) {
        String html = buildHtml(data);
        byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        log.debug("Generated HTML report, size={} bytes, scanId={}", bytes.length, data.getScanId());
        return bytes;
    }

    private String buildHtml(ReportData d) {
        StringBuilder sb = new StringBuilder();

        sb.append("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Sentinel Scan Report</title>
                  <style>
                    *{box-sizing:border-box;margin:0;padding:0}
                    body{font-family:'Segoe UI',Arial,sans-serif;background:#0f1117;color:#c9d1d9;padding:32px;line-height:1.5}
                    h1{color:#58a6ff;font-size:1.8rem;margin-bottom:4px}
                    h2{color:#79c0ff;font-size:1rem;text-transform:uppercase;letter-spacing:.08em;margin:28px 0 10px;border-bottom:1px solid #30363d;padding-bottom:6px}
                    .meta{color:#8b949e;font-size:.85rem;margin-bottom:28px}
                    .card{background:#161b22;border:1px solid #30363d;border-radius:8px;padding:20px;margin-bottom:16px}
                    .kv{display:flex;flex-wrap:wrap;gap:20px}
                    .kv-item label{display:block;color:#8b949e;font-size:.72rem;text-transform:uppercase;letter-spacing:.06em}
                    .kv-item span{font-size:.9rem;margin-top:2px;display:block}
                    .badge{display:inline-block;padding:2px 10px;border-radius:12px;font-size:.75rem;font-weight:600}
                    .b-up{background:#1a4731;color:#3fb950}
                    .b-open{background:#1a4731;color:#3fb950}
                    .b-tls{background:#1c3a5c;color:#58a6ff}
                    .b-fail{background:#4a1e1e;color:#f85149}
                    table{width:100%;border-collapse:collapse;font-size:.875rem}
                    th{text-align:left;padding:9px 14px;background:#21262d;color:#8b949e;font-weight:600;font-size:.78rem;text-transform:uppercase}
                    td{padding:9px 14px;border-bottom:1px solid #21262d;vertical-align:top}
                    tr:last-child td{border-bottom:none}
                    code{font-family:monospace;font-size:.78rem;color:#d2a8ff}
                    .script-block{margin-top:8px;padding:10px;background:#0d1117;border-radius:4px;border-left:3px solid #30363d}
                    .script-id{color:#d2a8ff;font-size:.8rem;font-weight:600;font-family:monospace}
                    .script-out{color:#8b949e;font-size:.77rem;white-space:pre-wrap;word-break:break-all;margin-top:4px}
                    details summary{cursor:pointer;color:#58a6ff;font-size:.82rem;padding:6px 0}
                    .args{font-family:monospace;font-size:.75rem;color:#8b949e;margin-top:8px;word-break:break-all}
                    footer{margin-top:40px;color:#484f58;font-size:.78rem;text-align:center}
                  </style>
                </head>
                <body>
                """);

        // Title
        sb.append("<h1>&#x1F6E1;&#xFE0F; Sentinel Scan Report</h1>\n");
        sb.append("<div class=\"meta\">Scan ID: <code>").append(d.getScanId())
                .append("</code> &nbsp;&bull;&nbsp; Target: <strong>").append(esc(d.getTarget()))
                .append("</strong></div>\n");

        // Scan metadata
        sb.append("<h2>Scan Metadata</h2>\n<div class=\"card\"><div class=\"kv\">\n");
        kv(sb, "Started", d.getScanStarted());
        kv(sb, "Finished", d.getScanFinished());
        kv(sb, "Duration", d.getElapsedSeconds() != null ? d.getElapsedSeconds() + "s" : null);
        kv(sb, "Nmap Version", d.getNmapVersion());
        sb.append("</div>\n");
        if (d.getNmapArgs() != null) {
            sb.append("<div class=\"args\">").append(esc(d.getNmapArgs())).append("</div>\n");
        }
        sb.append("</div>\n");

        // Host info
        ReportData.HostInfo hi = d.getHostInfo();
        if (hi != null) {
            sb.append("<h2>Host</h2>\n<div class=\"card\"><div class=\"kv\">\n");
            String bc = "up".equals(hi.getState()) ? "b-up" : "b-fail";
            sb.append("<div class=\"kv-item\"><label>State</label>")
                    .append("<span><span class=\"badge ").append(bc).append("\">")
                    .append(esc(hi.getState())).append("</span></span></div>\n");
            kv(sb, "Reason", hi.getReason());
            kv(sb, "Hosts Up", String.valueOf(hi.getHostsUp()));
            kv(sb, "Hosts Down", String.valueOf(hi.getHostsDown()));
            sb.append("</div>\n");
            if (hi.getAddresses() != null && !hi.getAddresses().isEmpty()) {
                sb.append("<div style=\"margin-top:12px\"><label style=\"color:#8b949e;font-size:.72rem;text-transform:uppercase\">Addresses</label><div style=\"margin-top:4px\">");
                hi.getAddresses().forEach(a -> sb.append("<code style=\"margin-right:10px\">").append(esc(a)).append("</code>"));
                sb.append("</div></div>\n");
            }
            sb.append("</div>\n");
        }

        // Open ports
        List<ReportData.PortInfo> ports = d.getOpenPorts();
        sb.append("<h2>Open Ports (").append(ports != null ? ports.size() : 0).append(")</h2>\n");

        if (ports == null || ports.isEmpty()) {
            sb.append("<div class=\"card\"><em style=\"color:#8b949e\">No open ports found.</em></div>\n");
        } else {
            sb.append("<div class=\"card\">\n<table>\n<thead><tr>")
                    .append("<th>Port</th><th>Protocol</th><th>Service</th>")
                    .append("<th>Product / Version</th><th>CPE</th></tr></thead>\n<tbody>\n");

            for (ReportData.PortInfo p : ports) {
                sb.append("<tr>");
                sb.append("<td><strong>").append(p.getPortId()).append("</strong>")
                        .append(" <span class=\"badge b-open\">open</span>");
                if (p.isTlsEnabled()) sb.append(" <span class=\"badge b-tls\">TLS</span>");
                sb.append("</td>");
                sb.append("<td>").append(esc(p.getProtocol())).append("</td>");
                sb.append("<td>").append(esc(p.getServiceName())).append("</td>");

                String pv = join(" ", p.getProduct(), p.getVersion());
                if (p.getExtraInfo() != null) pv += " (" + p.getExtraInfo() + ")";
                sb.append("<td>").append(esc(pv.trim())).append("</td>");

                sb.append("<td>");
                if (p.getCpes() != null) {
                    p.getCpes().forEach(c -> sb.append("<code style=\"display:block\">").append(esc(c)).append("</code>"));
                }
                sb.append("</td></tr>\n");

                // Scripts as expandable block
                if (p.getScripts() != null && !p.getScripts().isEmpty()) {
                    sb.append("<tr><td colspan=\"5\"><details><summary>Scripts (")
                            .append(p.getScripts().size()).append(")</summary>\n");
                    for (ReportData.ScriptInfo s : p.getScripts()) {
                        sb.append("<div class=\"script-block\">")
                                .append("<div class=\"script-id\">").append(esc(s.getId())).append("</div>")
                                .append("<div class=\"script-out\">").append(esc(s.getOutput())).append("</div>")
                                .append("</div>\n");
                    }
                    sb.append("</details></td></tr>\n");
                }
            }
            sb.append("</tbody></table></div>\n");
        }

        sb.append("<footer>Generated by Sentinel report-service</footer>\n");
        sb.append("</body></html>");
        return sb.toString();
    }

    private static void kv(StringBuilder sb, String label, String value) {
        sb.append("<div class=\"kv-item\"><label>").append(label).append("</label>")
                .append("<span>").append(value != null ? esc(value) : "—").append("</span></div>\n");
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String join(String sep, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (p != null && !p.isBlank()) {
                if (!sb.isEmpty()) sb.append(sep);
                sb.append(p);
            }
        }
        return sb.toString();
    }
}