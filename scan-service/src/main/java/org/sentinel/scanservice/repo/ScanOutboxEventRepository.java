package org.sentinel.scanservice.repo;

import org.sentinel.scanservice.model.ScanOutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface ScanOutboxEventRepository extends JpaRepository<ScanOutboxEvent, UUID> {

    // Primary polling query — index on (published, createdAt) makes this fast
    List<ScanOutboxEvent> findTop100ByPublishedFalseOrderByCreatedAtAsc();

    // Cleanup: delete rows published more than N days ago
    // Only runs on rows where published=true AND publishedAt is set
    @Modifying
    @Query("DELETE FROM ScanOutboxEvent e WHERE e.published = true AND e.publishedAt < :before")
    int deletePublishedBefore(@Param("before") Instant before);

    // Monitoring: how many events are stuck?
    long countByPublishedFalse();
}