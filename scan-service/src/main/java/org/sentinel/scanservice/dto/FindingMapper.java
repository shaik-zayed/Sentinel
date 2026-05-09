package org.sentinel.scanservice.dto;

import org.sentinel.scanservice.model.Finding;
import org.springframework.stereotype.Component;

@Component
public class FindingMapper {

    public FindingResponse toResponse(Finding finding) {
        return FindingResponse.builder()
                .findingId(finding.getFindingId())
                .scanItemId(finding.getScanItemId())
                .port(finding.getPort())
                .protocol(finding.getProtocol())
                .serviceName(finding.getServiceName())
                .product(finding.getProduct())
                .version(finding.getVersion())
                .cpe(finding.getCpe())
                .cveId(finding.getCveId())
                .cvssScore(finding.getCvssScore())
                .severity(finding.getSeverity())
                .description(finding.getDescription())
                .build();
    }
}