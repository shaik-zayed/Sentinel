package org.sentinel.scan_service.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

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

    private String scanMode;            // "LIGHT" or "DEEP"

    // Scan Options
    private boolean detectServiceVersion; // Toggle: "Detect service version"
    private boolean detectOs;             // Toggle: "Detect operating system"

    // Protocol
    private String protocol;              // "TCP" or "UDP"

    // Port Selection
    private String portMode;              // "COMMON" or "LIST"
    private String portValue;             // "top-100", "top-1000", or "80,443,8080"
}