package org.sentinel.reportservice.model;

/**
 * Supported report output formats.
 * Maps directly to the ?format= query parameter.
 */
public enum ReportFormat {
    PDF,
    HTML,
    JSON,
    DOCX;

    public String extension() {
        return name().toLowerCase();
    }

    public String contentType() {
        return switch (this) {
            case PDF  -> "application/pdf";
            case HTML -> "text/html";
            case JSON -> "application/json";
            case DOCX -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        };
    }

    /**
     * MinIO object path for a given scanId and format.
     * Pattern: reports/{scanId}/report.{ext}
     */
    public String objectPath(String scanId) {
        return "reports/" + scanId + "/report." + extension();
    }
}