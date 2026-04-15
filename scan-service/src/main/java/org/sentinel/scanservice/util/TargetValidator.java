package org.sentinel.scanservice.util;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Validates and sanitizes scan targets to prevent SSRF and command injection.
 * Only allows valid public hostnames and IPv4 addresses.
 * Blocks private/reserved IP ranges.
 */
public final class TargetValidator {

    private TargetValidator() {
        // Utility class
    }

    /**
     * Valid hostname: RFC 952/1123 compliant domain name.
     */
    private static final Pattern VALID_HOSTNAME = Pattern.compile(
            "^([a-zA-Z0-9]([a-zA-Z0-9\\-]{0,61}[a-zA-Z0-9])?\\.)+[a-zA-Z]{2,}$");

    /**
     * Valid IPv4 address (four octets, 0-255 each).
     */
    private static final Pattern VALID_IPV4 = Pattern.compile(
            "^((25[0-5]|(2[0-4]|1\\d|[1-9]|)\\d)\\.?\\b){4}$");

    /**
     * Blocked private/reserved IPv4 ranges (RFC 1918 + loopback).
     */
    private static final List<Pattern> BLOCKED_RANGES = List.of(
            Pattern.compile("^10\\..*"), // 10.0.0.0/8
            Pattern.compile("^172\\.(1[6-9]|2\\d|3[01])\\..*"), // 172.16.0.0/12
            Pattern.compile("^192\\.168\\..*"), // 192.168.0.0/16
            Pattern.compile("^127\\..*"), // 127.0.0.0/8 (loopback)
            Pattern.compile("^0\\..*"), // 0.0.0.0/8
            Pattern.compile("^169\\.254\\..*"), // 169.254.0.0/16 (link-local)
            Pattern.compile("^100\\.(6[4-9]|[7-9]\\d|1[0-2]\\d)\\..*") // 100.64.0.0/10 (CGNAT)
    );

    /**
     * Validates that the target is a safe, non-private hostname or IPv4 address.
     *
     * @param target the scan target (hostname or IPv4)
     * @throws IllegalArgumentException if the target is null, blank, or invalid
     */
    public static void validate(String target) {
        if (target == null || target.isBlank()) {
            throw new IllegalArgumentException("Scan target must not be blank");
        }

        String trimmed = target.trim();

        // Reject if it contains shell metacharacters or spaces
        if (trimmed.contains(" ") || trimmed.contains(";") || trimmed.contains("|")
                || trimmed.contains("&") || trimmed.contains("`") || trimmed.contains("$")
                || trimmed.contains("(") || trimmed.contains(")")) {
            throw new IllegalArgumentException("Scan target contains invalid characters: " + trimmed);
        }

        boolean isIp = VALID_IPV4.matcher(trimmed).matches();
        boolean isHostname = VALID_HOSTNAME.matcher(trimmed).matches();

        if (!isIp && !isHostname) {
            throw new IllegalArgumentException("Scan target is not a valid hostname or IPv4 address: " + trimmed);
        }

        // Block private/reserved IP ranges
        if (isIp) {
            for (Pattern blocked : BLOCKED_RANGES) {
                if (blocked.matcher(trimmed).matches()) {
                    throw new IllegalArgumentException(
                            "Scan target is a private/reserved IP address and cannot be scanned: " + trimmed);
                }
            }
        }
    }
}