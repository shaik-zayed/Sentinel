package org.sentinel.scanservice.repo;

import org.sentinel.scanservice.model.Finding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface FindingRepository extends JpaRepository<Finding, UUID> {

    List<Finding> findByScanItemIdOrderByCvssScoreDesc(UUID scanItemId);

    // Bulk delete – no entity loading
    @Modifying
    @Query("DELETE FROM Finding f WHERE f.scanItemId = :scanItemId")
    void deleteByScanItemId(@Param("scanItemId") UUID scanItemId);

    boolean existsByScanItemId(UUID scanItemId);
}