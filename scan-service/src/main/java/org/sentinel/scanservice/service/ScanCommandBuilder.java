package org.sentinel.scanservice.service;

import org.sentinel.scanservice.model.ScanRequest;
import org.sentinel.scanservice.model.enums.Protocol;
import org.sentinel.scanservice.model.enums.ScanMode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class ScanCommandBuilder {

    private static final Pattern PORT_TOKEN = Pattern.compile("^\\d{1,5}(-\\d{1,5})?$");
//    private static final int MAX_PORT_TOKENS = 1000;

    public List<String> buildCommand(ScanRequest scanRequest) {
        List<String> cmd = new ArrayList<>();

        // 1. Scan type
        if (scanRequest.getProtocol() == Protocol.UDP) {
            cmd.add("-sU");
        } else {
            cmd.add("-sS");
        }

        // 2. Deep mode (must be rejected for UDP)
        if (scanRequest.getScanMode() == ScanMode.DEEP) {
            if (scanRequest.getProtocol() == Protocol.UDP) {
                throw new IllegalArgumentException(
                        "Deep scan mode (which includes OS detection) is not supported with UDP scans.");
            }
            cmd.add("-T4");
            cmd.add("-A");   // -A implies -sV, -O, etc.
        } else {
            cmd.add("-T3");
            // Only add individual flags when NOT using -A (avoids redundancy)
            if (scanRequest.isDetectServiceVersion()) {
                cmd.add("-sV");
                cmd.add("--version-intensity");
                cmd.add("5");
            }
            if (scanRequest.isDetectOs()) {
                cmd.add("-O");
                cmd.add("--osscan-limit");
            }
        }

        // 3. Port specification
        if (scanRequest.getPortMode() != null) {
            switch (scanRequest.getPortMode()) { //Switch for future proofing
                case COMMON -> {
                    cmd.add("--top-ports");
                    String pv = scanRequest.getPortValue();
                    if (pv == null || !("top-100".equals(pv) || "top-1000".equals(pv))) {
                        throw new IllegalArgumentException(
                                "PortValue must be 'top-100' or 'top-1000' for COMMON mode");
                    }
                    cmd.add(pv.substring(4)); // "100" or "1000"
                }
                case LIST -> {
                    cmd.add("-p");
                    cmd.add(validatePortList(scanRequest.getPortValue()));
                }
            }
        }

        // 4. Output and target
        cmd.add("-oX");
        cmd.add("-");
        cmd.add(scanRequest.getTarget());

        return cmd;
    }

    private String validatePortList(String portValue) {
        if (portValue == null || portValue.isBlank()) {
            throw new IllegalArgumentException("PortValue is required for LIST mode");
        }

        String trimmed = portValue.trim();
        String[] tokens = trimmed.split(",");

        if (tokens.length == 0) {
            throw new IllegalArgumentException("Port list must contain at least one port");
        }

        // Optional length guard
//        if (tokens.length > MAX_PORT_TOKENS) {
//            throw new IllegalArgumentException("Too many port tokens");
//        }

        for (String token : tokens) {
            token = token.trim();
            if (!PORT_TOKEN.matcher(token).matches()) {
                throw new IllegalArgumentException(
                        "Invalid port spec '" + token + "'. " +
                                "Use individual ports (80,443), ranges (9000-10000), or a mix.");
            }

            // Validate numeric ranges
            String[] parts = token.split("-", 2);
            for (String part : parts) {
                int port = Integer.parseInt(part);
                if (port < 1 || port > 65535) {
                    throw new IllegalArgumentException(
                            "Port " + port + " out of range (1-65535)");
                }
            }
            if (parts.length == 2) {
                int start = Integer.parseInt(parts[0]);
                int end = Integer.parseInt(parts[1]);
                if (start >= end) {
                    throw new IllegalArgumentException(
                            "Invalid port range '" + token + "': start must be less than end");
                }
            }
        }

        return trimmed;
    }
}