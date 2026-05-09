package org.sentinel.scanservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.EnrichmentStatus;
import org.sentinel.scanservice.model.Finding;
import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.model.ServiceInfo;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentService {

    private final NmapXmlParser xmlParser;
    private final NvdClient nvdClient;
    private final NvdResponseMapper responseMapper;
    private final ScanItemRepository scanItemRepository;
    private final EnrichmentPersistence enrichmentPersistence;

    @Qualifier("nvdCallExecutor")
    private final Executor nvdCallExecutor;

    public void doEnrich(UUID scanItemId, String xmlOutput) {
        log.info("Enrichment started. ScanItemId: {}", scanItemId);
        enrichmentPersistence.markStatus(scanItemId, EnrichmentStatus.IN_PROGRESS);

        try {
            List<ServiceInfo> services = xmlParser.parse(xmlOutput);

            if (services.isEmpty()) {
                log.info("No qualifying services found. ScanItemId: {}", scanItemId);
                enrichmentPersistence.markStatus(scanItemId, EnrichmentStatus.NOT_APPLICABLE);
                return;
            }

            log.info("Found {} services to enrich. ScanItemId: {}", services.size(), scanItemId);

            AtomicInteger failureCount = new AtomicInteger(0);

            List<CompletableFuture<List<Finding>>> futures = services.stream()
                    .map(service -> CompletableFuture.supplyAsync(
                            () -> fetchAndMap(scanItemId, service, failureCount),
                            nvdCallExecutor
                    ))
                    .toList();

            List<Finding> allFindings = futures.stream()
                    .map(CompletableFuture::join)
                    .flatMap(List::stream)
                    .toList();

            int totalServices = services.size();
            int failed = failureCount.get();

            if (failed == totalServices) {
                log.error("All {} NVD calls failed. Marking FAILED. ScanItemId: {}",
                        totalServices, scanItemId);
                enrichmentPersistence.markStatus(scanItemId, EnrichmentStatus.FAILED);
            } else if (failed > 0) {
                log.warn("{}/{} NVD calls failed. Saving {} findings and marking PARTIAL. ScanItemId: {}",
                        failed, totalServices, allFindings.size(), scanItemId);
                enrichmentPersistence.saveFindingsWithStatus(
                        scanItemId, allFindings, EnrichmentStatus.PARTIAL);
            } else {
                enrichmentPersistence.saveFindings(scanItemId, allFindings);
            }

        } catch (Exception e) {
            log.error("Enrichment failed. ScanItemId: {}: {}", scanItemId, e.getMessage(), e);
            enrichmentPersistence.markStatus(scanItemId, EnrichmentStatus.FAILED);
        }
    }

    private List<Finding> fetchAndMap(UUID scanItemId, ServiceInfo service,
                                      AtomicInteger failureCount) {
        try {
            log.debug("Fetching CVEs. ScanItemId: {}, Product: {}, Version: {}",
                    scanItemId, service.product(), service.version());

            JsonNode response = nvdClient.fetchCves(
                    service.product(), service.version(), service.cpe()
            );

            if (response == null) {
                log.warn("NVD returned null for product={} version={}. " +
                                "Counting as failure. ScanItemId: {}",
                        service.product(), service.version(), scanItemId);
                failureCount.incrementAndGet();
                return List.of();
            }

            return responseMapper.map(
                    response, scanItemId,
                    service.port(), service.protocol(), service.serviceName(),
                    service.product(), service.version(), service.cpe()
            );

        } catch (Exception e) {
            log.error("CVE fetch failed. ScanItemId: {}, Product: {}, Version: {}: {}",
                    scanItemId, service.product(), service.version(), e.getMessage());
            failureCount.incrementAndGet();
            return List.of();
        }
    }
}