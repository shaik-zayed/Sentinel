package org.sentinel.scanservice.repo;

import org.sentinel.scanservice.model.EnrichmentStatus;
import org.sentinel.scanservice.model.ScanItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanItemRepository extends JpaRepository<ScanItem, UUID> {

    Optional<ScanItem> findByCorrelationId(String correlationId);

    List<ScanItem> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    List<ScanItem> findByEnrichmentStatus(EnrichmentStatus enrichmentStatus);

    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ScanItem s SET s.enrichmentStatus = :status WHERE s.scanItemId = :scanItemId")
    void updateEnrichmentStatus(@Param("scanItemId") UUID scanItemId,
                                @Param("status") EnrichmentStatus status);
}