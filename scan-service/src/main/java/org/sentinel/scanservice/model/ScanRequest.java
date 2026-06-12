package org.sentinel.scanservice.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.sentinel.scanservice.model.enums.PortMode;
import org.sentinel.scanservice.model.enums.Protocol;
import org.sentinel.scanservice.model.enums.ScanMode;

import java.util.UUID;

@Data
@Entity
@Builder
@RequiredArgsConstructor
@AllArgsConstructor
public class ScanRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID scanRequestId;

    @NotBlank(message = "Target is required")
    private String target;              // "192.168.1.1" or "example.com"

    @NotNull(message = "scanMode is required")
    @Enumerated(EnumType.STRING)
    private ScanMode scanMode;

    private boolean detectServiceVersion; // Toggle: "Detect service version"
    private boolean detectOs;             // Toggle: "Detect operating system"

    @NotNull(message = "protocol is required")
    @Enumerated(EnumType.STRING)
    private Protocol protocol;

    @NotNull(message = "portMode is required")
    @Enumerated(EnumType.STRING)
    private PortMode portMode;

    private String portValue;
}