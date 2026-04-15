package org.sentinel.nmapservice.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScanCommandMessage {
    private String correlationId;
    private UUID userId;
    private UUID scanRequestId;
    private UUID scanItemId;
    private List<String> command;
    private String target;
    private Instant timestamp;
    private String replyTopic;
    private String scanMode;
    private String protocol;
}