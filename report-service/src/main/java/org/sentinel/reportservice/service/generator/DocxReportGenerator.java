package org.sentinel.reportservice.service.generator;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblPr;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTTblWidth;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.STTblWidth;
import org.sentinel.reportservice.dto.CveFinding;
import org.sentinel.reportservice.dto.ReportData;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.util.List;

/**
 * Generates a DOCX report using Apache POI.
 * Produces a clean, readable Word document with tables and headings.
 */
@Slf4j
@Component
public class DocxReportGenerator {

    public byte[] generate(ReportData data) {
        try (XWPFDocument doc = new XWPFDocument();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            // ---- Title ----
            XWPFParagraph title = doc.createParagraph();
            title.setStyle("Title");
            XWPFRun titleRun = title.createRun();
            titleRun.setText("Sentinel Scan Report");
            titleRun.setBold(true);
            titleRun.setFontSize(20);

            // Scan ID + target subtitle
            addParagraph(doc, "Scan ID: " + data.getScanId()
                    + "    Target: " + nullSafe(data.getTarget()), 9, false);
            addSpacer(doc);

            // ---- Metadata ----
            addHeading(doc, "Scan Metadata", 1);
            XWPFTable metaTable = createTable(doc, 2);
            addTableRow(metaTable, "Started",      data.getScanStarted());
            addTableRow(metaTable, "Finished",     data.getScanFinished());
            addTableRow(metaTable, "Duration",
                    data.getElapsedSeconds() != null ? data.getElapsedSeconds() + " seconds" : null);
            addTableRow(metaTable, "Nmap Version", data.getNmapVersion());
            if (data.getNmapArgs() != null) {
                addTableRow(metaTable, "Command", data.getNmapArgs());
            }
            addSpacer(doc);

            // ---- Host ----
            ReportData.HostInfo hi = data.getHostInfo();
            if (hi != null) {
                addHeading(doc, "Host", 1);
                XWPFTable hostTable = createTable(doc, 2);
                addTableRow(hostTable, "State",      hi.getState());
                addTableRow(hostTable, "Reason",     hi.getReason());
                addTableRow(hostTable, "Hosts Up",   String.valueOf(hi.getHostsUp()));
                addTableRow(hostTable, "Hosts Down", String.valueOf(hi.getHostsDown()));
                if (hi.getAddresses() != null && !hi.getAddresses().isEmpty()) {
                    addTableRow(hostTable, "Addresses", String.join(", ", hi.getAddresses()));
                }
                if (hi.getHostnames() != null && !hi.getHostnames().isEmpty()) {
                    addTableRow(hostTable, "Hostnames", String.join(", ", hi.getHostnames()));
                }
                addSpacer(doc);
            }

            // ---- Open Ports ----
            List<ReportData.PortInfo> ports = data.getOpenPorts();
            addHeading(doc, "Open Ports (" + (ports != null ? ports.size() : 0) + ")", 1);

            if (ports == null || ports.isEmpty()) {
                addParagraph(doc, "No open ports found.", 10, false);
            } else {
                XWPFTable portTable = createTable(doc, 5);

                // Header
                XWPFTableRow header = portTable.getRow(0);
                setCell(header, 0, "Port",             true);
                setCell(header, 1, "Protocol",         true);
                setCell(header, 2, "Service",          true);
                setCell(header, 3, "Product / Version",true);
                setCell(header, 4, "CPE",              true);

                for (ReportData.PortInfo p : ports) {
                    XWPFTableRow row = portTable.createRow();
                    String pv = join(" ", p.getProduct(), p.getVersion());
                    if (p.getExtraInfo() != null) pv += " (" + p.getExtraInfo() + ")";
                    String cpe = p.getCpes() != null ? String.join(", ", p.getCpes()) : "";
                    String portLabel = p.getPortId() + (p.isTlsEnabled() ? " (TLS)" : "");

                    setCell(row, 0, portLabel,          false);
                    setCell(row, 1, p.getProtocol(),    false);
                    setCell(row, 2, p.getServiceName(), false);
                    setCell(row, 3, pv.trim(),          false);
                    setCell(row, 4, cpe,                false);
                }
                addSpacer(doc);

                // Scripts
                boolean hasScripts = ports.stream()
                        .anyMatch(p -> p.getScripts() != null && !p.getScripts().isEmpty());

                if (hasScripts) {
                    addHeading(doc, "Script Output", 1);
                    for (ReportData.PortInfo p : ports) {
                        if (p.getScripts() == null || p.getScripts().isEmpty()) continue;
                        addHeading(doc, "Port " + p.getPortId(), 2);
                        for (ReportData.ScriptInfo s : p.getScripts()) {
                            addParagraph(doc, nullSafe(s.getId()), 9, true);
                            if (s.getOutput() != null) {
                                addParagraph(doc, s.getOutput(), 8, false);
                            }
                        }
                    }
                }
            }

            // ── CVE Findings ─────────────────────────────────────────────────────
            List<CveFinding> findings = data.getCveFindings();
            if (findings != null && !findings.isEmpty()) {
                addSpacer(doc);
                addHeading(doc, "CVE Findings (" + findings.size() + ")", 1);
                XWPFTable cveTable = createTable(doc, 7);

                XWPFTableRow header = cveTable.getRow(0);
                setCell(header, 0, "Port", true);
                setCell(header, 1, "Service", true);
                setCell(header, 2, "Product/Version", true);
                setCell(header, 3, "CVE ID", true);
                setCell(header, 4, "CVSS", true);
                setCell(header, 5, "Severity", true);
                setCell(header, 6, "Description", true);

                for (CveFinding f : findings) {
                    XWPFTableRow row = cveTable.createRow();
                    setCell(row, 0, f.getPort() + " (" + f.getProtocol() + ")", false);
                    setCell(row, 1, nullSafe(f.getServiceName()), false);
                    String pv = join(" ", f.getProduct(), f.getVersion());
                    setCell(row, 2, pv, false);
                    setCell(row, 3, nullSafe(f.getCveId()), false);
                    setCell(row, 4, f.getCvssScore() != null ? String.format("%.1f", f.getCvssScore()) : "—", false);
                    setCell(row, 5, nullSafe(f.getSeverity()), false);
                    setCell(row, 6, nullSafe(f.getDescription()), false);
                }
            }

            // ---- Footer ----
            addSpacer(doc);
            addParagraph(doc, "Generated by Sentinel report-service", 8, false);

            doc.write(baos);
            byte[] bytes = baos.toByteArray();
            log.debug("Generated DOCX report, size={} bytes, scanId={}", bytes.length, data.getScanId());
            return bytes;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate DOCX report: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------

    private void addHeading(XWPFDocument doc, String text, int level) {
        XWPFParagraph p = doc.createParagraph();
        p.setStyle("Heading" + level);
        p.createRun().setText(text);
    }

    private void addParagraph(XWPFDocument doc, String text, int fontSize, boolean bold) {
        XWPFParagraph p = doc.createParagraph();
        XWPFRun run = p.createRun();
        run.setText(text != null ? text : "");
        run.setFontSize(fontSize);
        run.setBold(bold);
    }

    private void addSpacer(XWPFDocument doc) {
        doc.createParagraph().createRun().setText("");
    }

    private XWPFTable createTable(XWPFDocument doc, int cols) {
        XWPFTable table = doc.createTable(1, cols);
        // Set table to use full page width
        CTTblPr tblPr = table.getCTTbl().getTblPr();
        CTTblWidth tblWidth = tblPr.addNewTblW();
        tblWidth.setType(STTblWidth.PCT);
        tblWidth.setW(BigInteger.valueOf(5000));
        return table;
    }

    private void addTableRow(XWPFTable table, String label, String value) {
        XWPFTableRow row = table.createRow();
        setCell(row, 0, label, true);
        setCell(row, 1, value != null ? value : "—", false);
    }

    private void setCell(XWPFTableRow row, int index, String text, boolean bold) {
        XWPFTableCell cell = row.getCell(index);
        if (cell == null) cell = row.addNewTableCell();
        XWPFParagraph p = cell.getParagraphs().isEmpty()
                ? cell.addParagraph() : cell.getParagraphs().get(0);
        if (p.getRuns().isEmpty()) {
            XWPFRun run = p.createRun();
            run.setText(text != null ? text : "");
            run.setBold(bold);
            run.setFontSize(9);
        } else {
            XWPFRun run = p.getRuns().get(0);
            run.setText(text != null ? text : "");
            run.setBold(bold);
        }
    }

    private static String nullSafe(String s) { return s != null ? s : "—"; }

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