package org.sentinel.scanservice.model;

import org.sentinel.scanservice.dto.FindingResponse;

import java.util.List;
import java.util.UUID;

/**
 * Wrapper response that includes enrichment status alongside the findings list.
 * This lets callers know if the list is complete, still in progress, or failed.
 */
public record FindingsResponse(
        UUID scanItemId,
        String enrichmentStatus,
        List<FindingResponse> findings
) {}
