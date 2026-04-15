package org.sentinel.scanservice.service;

import org.sentinel.scanservice.model.ScanRequest;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
public class ScanCommandBuilder {

    public List<String> buildCommand(ScanRequest scanRequest) {
        List<String> cmd = new ArrayList<>();

        if ("UDP".equalsIgnoreCase(scanRequest.getProtocol())) {
            cmd.add("-sU");
        } else {
            cmd.add("-sS");
        }

        if (scanRequest.isDetectServiceVersion()) {
            cmd.add("-sV");
            cmd.add("--version-intensity");
            cmd.add("5");
        }

        if (scanRequest.isDetectOs()) {
            cmd.add("-O");
            cmd.add("--osscan-limit");
        }

        if (scanRequest.getPortMode() != null) {
            if ("COMMON".equalsIgnoreCase(scanRequest.getPortMode())) {
                cmd.add("--top-ports");
                cmd.add("top-1000".equals(scanRequest.getPortValue()) ? "1000" : "100");

            } else if ("LIST".equalsIgnoreCase(scanRequest.getPortMode())) {
                String portValue = scanRequest.getPortValue();

                if (portValue == null || portValue.isBlank()) {
                    throw new IllegalArgumentException("Port value required for LIST mode");
                }

                String sanitized = portValue.replaceAll("[^0-9,-]", "");

                if (sanitized.isEmpty()) {
                    throw new IllegalArgumentException("Invalid port value: " + portValue);
                }

                sanitized = sanitized.replaceAll(",+", ",").replaceAll("^,|,$", "");

                if (sanitized.isEmpty()) {
                    throw new IllegalArgumentException("Port value empty after sanitization");
                }

                cmd.add("-p");
                cmd.add(sanitized);
            }
        }

        if ("DEEP".equalsIgnoreCase(scanRequest.getScanMode())) {
            cmd.add("-T4");
            cmd.add("-A");
        } else {
            cmd.add("-T3");
        }

        cmd.add("-oX");
        cmd.add("-");
        cmd.add(scanRequest.getTarget());

        return cmd;
    }
}