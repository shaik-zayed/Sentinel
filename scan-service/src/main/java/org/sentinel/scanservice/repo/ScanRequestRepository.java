package org.sentinel.scanservice.repo;

import org.sentinel.scanservice.model.ScanRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScanRequestRepository extends JpaRepository<ScanRequest, UUID> {

}
