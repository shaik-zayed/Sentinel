package org.sentinel.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;
import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ReportResponse {

    private UUID scanId;
    private String format;
    private String status;          // READY
    private String downloadUrl;     // pre-signed MinIO URL, valid for 1 hour
    private String expiresIn;       // "60 minutes"
    private String contentType;

    /**
     * Present only on GET /formats - shows which formats are already cached.
     * e.g. { "pdf": true, "html": false, "json": true, "docx": false }
     */
    private Map<String, Boolean> cachedFormats;
}