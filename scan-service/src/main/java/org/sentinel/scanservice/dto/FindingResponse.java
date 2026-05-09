package org.sentinel.scanservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FindingResponse {
    private UUID findingId;
    private UUID scanItemId;
    private int port;
    private String protocol;
    private String serviceName;
    private String product;
    private String version;
    private String cpe;
    private String cveId;
    private Double cvssScore;
    private String severity;
    private String description;
}