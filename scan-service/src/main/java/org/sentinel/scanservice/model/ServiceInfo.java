package org.sentinel.scanservice.model;

/**
 * Represents a single open port with a detected service and version.
 * Only ports with both product and version populated are returned —
 * these are the only ones we can meaningfully look up in NVD.
 */
public record ServiceInfo(
        int port,
        String protocol,
        String serviceName,
        String product,
        String version,
        String cpe
) {
}
