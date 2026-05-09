package org.sentinel.scanservice.service;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.model.EnrichmentStatus;
import org.sentinel.scanservice.model.Finding;
import org.sentinel.scanservice.repo.FindingRepository;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnrichmentPersistence {

    private final FindingRepository findingRepository;
    private final ScanItemRepository scanItemRepository;
    private final EntityManager entityManager;

    @Transactional
    public void saveFindings(UUID scanItemId, List<Finding> findings) {
        persistFindings(scanItemId, findings);
        scanItemRepository.updateEnrichmentStatus(scanItemId, EnrichmentStatus.COMPLETED);
        log.info("Enrichment COMPLETED. ScanItemId: {}, findings saved: {}",
                scanItemId, findings.size());
    }

    @Transactional
    public void saveFindingsWithStatus(UUID scanItemId, List<Finding> findings,
                                       EnrichmentStatus status) {
        persistFindings(scanItemId, findings);
        scanItemRepository.updateEnrichmentStatus(scanItemId, status);
        log.info("Enrichment {} (partial). ScanItemId: {}, findings saved: {}",
                status, scanItemId, findings.size());
    }

    @Transactional
    public void markStatus(UUID scanItemId, EnrichmentStatus status) {
        log.debug("Marking enrichment status: {} for ScanItemId: {}", status, scanItemId);
        scanItemRepository.updateEnrichmentStatus(scanItemId, status);
    }

    private void persistFindings(UUID scanItemId, List<Finding> findings) {
        findingRepository.deleteByScanItemId(scanItemId);

        if (!findings.isEmpty()) {
            findingRepository.saveAll(findings);
            entityManager.flush();
            log.info("Saved {} findings. ScanItemId: {}", findings.size(), scanItemId);
        } else {
            log.info("No CVEs to save. ScanItemId: {}", scanItemId);
        }
    }
}