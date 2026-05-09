package org.sentinel.scanservice.model;

public enum EnrichmentStatus {
    PENDING,        // enrichment not yet started
    IN_PROGRESS,    // enrichment currently running
    COMPLETED,      // all CVEs fetched and saved (findings list is final)
    PARTIAL,        // some services fetched; at least one NVD call failed — list incomplete
    FAILED,         // enrichment failed after all retries — no findings saved
    NOT_APPLICABLE  // scan had no identifiable services (no product/version detected)
}