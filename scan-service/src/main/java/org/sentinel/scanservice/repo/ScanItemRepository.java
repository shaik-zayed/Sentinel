package org.sentinel.scanservice.repo;

import org.sentinel.scanservice.model.EnrichmentStatus;
import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.model.ScanStatus;
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

    // Conditional status transition for the outbox publisher.
    // Returns 1 if the update succeeded (status was `from`), 0 if it was already something else.
    // This is an atomic compare-and-swap at the database level.
    // Safe against concurrent publishers: only one can win the UPDATE for a given scan.
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE ScanItem s SET s.scanStatus = :to WHERE s.scanItemId = :scanItemId AND s.scanStatus = :from")
    int transitionScanStatus(
            @Param("scanItemId") UUID scanItemId,
            @Param("from") ScanStatus from,
            @Param("to") ScanStatus to
    );

    Optional<ScanItem> findByUserIdAndIdempotencyKey(UUID userId, String idempotencyKey);
}