package org.sentinel.nmap_service.model;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ScanRequest {
    private UUID scanId;
    private String target;
    private String scanMode;
    private boolean detectServiceVersion;
    private boolean detectOs;
    private String protocol;
    private String portMode;
    private String portValue;
}