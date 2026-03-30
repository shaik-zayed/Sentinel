package org.sentinel.scan_service.repo;

import org.sentinel.scan_service.model.ScanItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScanItemRepository extends JpaRepository<ScanItem, UUID> {
    Optional<ScanItem> findByCorrelationId(String correlationId);

    List<ScanItem> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}