package org.sentinel.scan_service.repo;

import org.sentinel.scan_service.model.ScanRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ScanRequestRepository extends JpaRepository<ScanRequest, UUID> {

}
