package org.sentinel.reportservice.exception;

public class EnrichmentPendingException extends RuntimeException {
    private final String enrichmentStatus;

    public EnrichmentPendingException(String enrichmentStatus) {
        super("CVE enrichment not yet complete. Status: " + enrichmentStatus);
        this.enrichmentStatus = enrichmentStatus;
    }

    public String getEnrichmentStatus() {
        return enrichmentStatus;
    }
}