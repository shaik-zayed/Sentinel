package org.sentinel.reportservice.service.generator;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.dto.ReportData;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/**
 * Generates a PDF report using iText7.
 * Uses standard built-in fonts — no external font files required.
 */
@Slf4j
@Component
public class PdfReportGenerator {

    // Colours matching the dark theme but readable on white paper
    private static final DeviceRgb HEADING_BLUE   = new DeviceRgb(33,  97, 140);
    private static final DeviceRgb ACCENT_GREEN   = new DeviceRgb(39, 174,  96);
    private static final DeviceRgb ACCENT_RED     = new DeviceRgb(192,  57,  43);
    private static final DeviceRgb LABEL_GREY     = new DeviceRgb(108, 117, 125);
    private static final DeviceRgb TABLE_HEADER   = new DeviceRgb(52,  73,  94);
    private static final DeviceRgb TABLE_ROW_ALT  = new DeviceRgb(245, 248, 250);

    public byte[] generate(ReportData data) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document doc = new Document(pdf);

            PdfFont regular  = PdfFontFactory.createFont("Helvetica");
            PdfFont bold     = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont mono     = PdfFontFactory.createFont("Courier");

            // ---- Title ----
            doc.add(new Paragraph("Sentinel Scan Report")
                    .setFont(bold).setFontSize(20).setFontColor(HEADING_BLUE)
                    .setMarginBottom(4));

            doc.add(new Paragraph("Scan ID: " + data.getScanId()
                    + "   |   Target: " + nullSafe(data.getTarget()))
                    .setFont(regular).setFontSize(9).setFontColor(LABEL_GREY)
                    .setMarginBottom(20));

            // ---- Scan Metadata ----
            addSectionHeading(doc, bold, "Scan Metadata");
            Table metaTable = new Table(UnitValue.createPercentArray(new float[]{2, 3}))
                    .useAllAvailableWidth().setMarginBottom(16);
            addMetaRow(metaTable, regular, bold, "Started",       data.getScanStarted());
            addMetaRow(metaTable, regular, bold, "Finished",      data.getScanFinished());
            addMetaRow(metaTable, regular, bold, "Duration",
                    data.getElapsedSeconds() != null ? data.getElapsedSeconds() + " seconds" : null);
            addMetaRow(metaTable, regular, bold, "Nmap Version",  data.getNmapVersion());
            doc.add(metaTable);

            if (data.getNmapArgs() != null) {
                doc.add(new Paragraph("Command: " + data.getNmapArgs())
                        .setFont(mono).setFontSize(7).setFontColor(LABEL_GREY)
                        .setMarginBottom(16));
            }

            // ---- Host ----
            ReportData.HostInfo hi = data.getHostInfo();
            if (hi != null) {
                addSectionHeading(doc, bold, "Host");
                Table hostTable = new Table(UnitValue.createPercentArray(new float[]{2, 3}))
                        .useAllAvailableWidth().setMarginBottom(16);
                addMetaRow(hostTable, regular, bold, "State",
                        hi.getState(), "up".equals(hi.getState()) ? ACCENT_GREEN : ACCENT_RED);
                addMetaRow(hostTable, regular, bold, "Reason",     hi.getReason());
                addMetaRow(hostTable, regular, bold, "Hosts Up",   String.valueOf(hi.getHostsUp()));
                addMetaRow(hostTable, regular, bold, "Hosts Down", String.valueOf(hi.getHostsDown()));
                if (hi.getAddresses() != null && !hi.getAddresses().isEmpty()) {
                    addMetaRow(hostTable, regular, bold, "Addresses",
                            String.join(", ", hi.getAddresses()));
                }
                doc.add(hostTable);
            }

            // ---- Open Ports ----
            List<ReportData.PortInfo> ports = data.getOpenPorts();
            addSectionHeading(doc, bold,
                    "Open Ports (" + (ports != null ? ports.size() : 0) + ")");

            if (ports == null || ports.isEmpty()) {
                doc.add(new Paragraph("No open ports found.")
                        .setFont(regular).setFontSize(9).setFontColor(LABEL_GREY));
            } else {
                Table portTable = new Table(
                        UnitValue.createPercentArray(new float[]{1, 1, 2, 3, 3}))
                        .useAllAvailableWidth().setMarginBottom(16);

                // Header row
                for (String h : new String[]{"Port", "Protocol", "Service", "Product / Version", "CPE"}) {
                    portTable.addHeaderCell(new Cell().add(new Paragraph(h)
                                    .setFont(bold).setFontSize(8).setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(TABLE_HEADER).setPadding(5));
                }

                boolean alt = false;
                for (ReportData.PortInfo p : ports) {
                    DeviceRgb bg = alt ? TABLE_ROW_ALT : null;
                    alt = !alt;

                    String pv = join(" ", p.getProduct(), p.getVersion());
                    if (p.getExtraInfo() != null) pv += " (" + p.getExtraInfo() + ")";
                    String cpe = p.getCpes() != null ? String.join("\n", p.getCpes()) : "";

                    String portLabel = p.getPortId()
                            + (p.isTlsEnabled() ? " (TLS)" : "");

                    addPortCell(portTable, bold,   portLabel,          bg, ACCENT_GREEN);
                    addPortCell(portTable, regular, p.getProtocol(),    bg, null);
                    addPortCell(portTable, regular, p.getServiceName(), bg, null);
                    addPortCell(portTable, regular, pv.trim(),          bg, null);
                    addPortCell(portTable, mono,    cpe,                bg, LABEL_GREY);
                }
                doc.add(portTable);

                // Script details below the table
                for (ReportData.PortInfo p : ports) {
                    if (p.getScripts() == null || p.getScripts().isEmpty()) continue;

                    doc.add(new Paragraph("Port " + p.getPortId() + " — Script Output")
                            .setFont(bold).setFontSize(9).setFontColor(HEADING_BLUE)
                            .setMarginTop(8).setMarginBottom(4));

                    for (ReportData.ScriptInfo s : p.getScripts()) {
                        doc.add(new Paragraph(nullSafe(s.getId()))
                                .setFont(bold).setFontSize(8).setFontColor(LABEL_GREY)
                                .setMarginBottom(2));
                        if (s.getOutput() != null) {
                            doc.add(new Paragraph(s.getOutput())
                                    .setFont(mono).setFontSize(7).setFontColor(LABEL_GREY)
                                    .setMarginBottom(6));
                        }
                    }
                }
            }

            // ---- Footer ----
            doc.add(new Paragraph("Generated by Sentinel report-service")
                    .setFont(regular).setFontSize(8).setFontColor(LABEL_GREY)
                    .setTextAlignment(TextAlignment.CENTER).setMarginTop(24));

            doc.close();

            byte[] bytes = baos.toByteArray();
            log.debug("Generated PDF report, size={} bytes, scanId={}", bytes.length, data.getScanId());
            return bytes;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF report: " + e.getMessage(), e);
        }
    }

    // -----------------------------------------------------------------------

    private void addSectionHeading(Document doc, PdfFont bold, String text) {
        doc.add(new Paragraph(text).setFont(bold).setFontSize(12)
                .setFontColor(HEADING_BLUE).setMarginBottom(6).setMarginTop(4));
    }

    private void addMetaRow(Table t, PdfFont regular, PdfFont bold,
                            String label, String value) {
        addMetaRow(t, regular, bold, label, value, null);
    }

    private void addMetaRow(Table t, PdfFont regular, PdfFont bold,
                            String label, String value, DeviceRgb valueColor) {
        t.addCell(new Cell().add(new Paragraph(label)
                .setFont(bold).setFontSize(9)).setPadding(4));
        Paragraph vp = new Paragraph(value != null ? value : "—")
                .setFont(regular).setFontSize(9);
        if (valueColor != null) vp.setFontColor(valueColor);
        t.addCell(new Cell().add(vp).setPadding(4));
    }

    private void addPortCell(Table t, PdfFont font, String text,
                             DeviceRgb bg, DeviceRgb color) {
        Paragraph p = new Paragraph(text != null ? text : "")
                .setFont(font).setFontSize(8);
        if (color != null) p.setFontColor(color);
        Cell cell = new Cell().add(p).setPadding(4);
        if (bg != null) cell.setBackgroundColor(bg);
        t.addCell(cell);
    }

    private static String nullSafe(String s) {
        return s != null ? s : "—";
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