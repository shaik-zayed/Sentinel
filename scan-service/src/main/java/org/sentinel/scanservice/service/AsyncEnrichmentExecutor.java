package org.sentinel.scanservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AsyncEnrichmentExecutor {

    private final EnrichmentService enrichmentService;

    @Async("enrichmentExecutor")
    public void enrich(UUID scanItemId, String xmlOutput) {
        enrichmentService.doEnrich(scanItemId, xmlOutput);
    }
}