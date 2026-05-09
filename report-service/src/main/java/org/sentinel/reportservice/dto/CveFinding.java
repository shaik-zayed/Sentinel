package org.sentinel.reportservice.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CveFinding {
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