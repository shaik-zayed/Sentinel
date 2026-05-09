package org.sentinel.scanservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.scanservice.dto.FindingMapper;
import org.sentinel.scanservice.dto.FindingResponse;
import org.sentinel.scanservice.exception.ForbiddenException;
import org.sentinel.scanservice.exception.NotFoundException;
import org.sentinel.scanservice.model.FindingsResponse;
import org.sentinel.scanservice.model.ScanItem;
import org.sentinel.scanservice.model.ScanStatus;
import org.sentinel.scanservice.repo.FindingRepository;
import org.sentinel.scanservice.repo.ScanItemRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FindingService {

    private final FindingRepository findingRepository;
    private final ScanItemRepository scanItemRepository;
    private final FindingMapper findingMapper;

    public FindingsResponse getFindings(UUID scanItemId, UUID userId) {
        ScanItem scanItem = scanItemRepository.findById(scanItemId)
                .orElseThrow(() -> new NotFoundException("Scan not found: " + scanItemId));

        if (!scanItem.getUserId().equals(userId)) {
            log.warn("Unauthorized findings access. ScanItemId: {}, UserId: {}", scanItemId, userId);
            throw new ForbiddenException("Access denied");
        }

        if (scanItem.getScanStatus() != ScanStatus.FINISHED) {
            throw new org.sentinel.scanservice.exception.BadRequestException(
                    "Scan not completed. Status: " + scanItem.getScanStatus());
        }

        List<FindingResponse> findings = findingRepository
                .findByScanItemIdOrderByCvssScoreDesc(scanItemId)
                .stream()
                .map(findingMapper::toResponse)
                .toList();

        log.info("Returning {} findings for ScanItemId: {}, EnrichmentStatus: {}",
                findings.size(), scanItemId, scanItem.getEnrichmentStatus());

        return new FindingsResponse(
                scanItemId,
                scanItem.getEnrichmentStatus().name(),
                findings
        );
    }
}