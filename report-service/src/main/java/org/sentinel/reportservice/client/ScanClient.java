package org.sentinel.reportservice.client;

import org.sentinel.reportservice.dto.ScanResultResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;

import java.util.UUID;

/**
 * Declarative HTTP client for scan-service.
 * Backed by RestClient + Spring Cloud LoadBalancer (Eureka).
 *
 * Maps to scan-service's GET /api/v1/scan/{scanId}/result endpoint.
 * That endpoint returns the raw nmap XML in ScanResponse.scanOutput,
 * but only when the scan status is FINISHED.
 * It throws BadRequestException (400) if the scan is not yet complete.
 * It throws ForbiddenException (403) if userId does not own the scan.
 * It throws NotFoundException (404) if the scanId does not exist.
 */
@HttpExchange("/api/v1/scan")
public interface ScanClient {

    @GetExchange("/{scanId}/result")
    ScanResultResponse getScanResult(
            @PathVariable UUID scanId,
            @RequestHeader("X-User-Id") String userId
    );
}