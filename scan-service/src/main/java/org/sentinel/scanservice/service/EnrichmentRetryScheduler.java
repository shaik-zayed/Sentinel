package org.sentinel.scanservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.EnrichmentStatus;
import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentRetryScheduler {

    private final ScanItemRepository scanItemRepository;
    private final AsyncEnrichmentExecutor asyncExecutor;

    @Scheduled(fixedDelay = 600_000)
    public void retryFailedEnrichments() {

        List<ScanItem> failedItems = scanItemRepository
                .findByEnrichmentStatus(EnrichmentStatus.FAILED)
                .stream()
                .filter(s -> s.getScanOutput() != null)
                .toList();

        if (failedItems.isEmpty()) {
            return;
        }

        log.info("Retrying {} failed enrichments", failedItems.size());

        failedItems.forEach(item ->
                asyncExecutor.enrich(
                        item.getScanItemId(),
                        item.getScanOutput()
                )
        );
    }

    @Scheduled(fixedDelay = 1_800_000)
    public void retryPartialEnrichments() {

        List<ScanItem> partialIds = scanItemRepository
                .findByEnrichmentStatus(EnrichmentStatus.PARTIAL)
                .stream()
                .filter(s -> s.getScanOutput() != null)
                .toList();

        if (partialIds.isEmpty()) {
            return;
        }

        log.info("Retrying {} partial enrichments", partialIds.size());

        partialIds.forEach(item ->
                asyncExecutor.enrich(
                        item.getScanItemId(),
                        item.getScanOutput())
        );
    }
}