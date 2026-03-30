package org.sentinel.reportservice.service.generator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.extern.slf4j.Slf4j;
import org.sentinel.reportservice.dto.ReportData;
import org.springframework.stereotype.Component;

/**
 * Serialises ReportData to a pretty-printed JSON byte array.
 * No external dependencies beyond Jackson (already on classpath).
 */
@Slf4j
@Component
public class JsonReportGenerator {

    private final ObjectMapper mapper;

    public JsonReportGenerator() {
        this.mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .enable(SerializationFeature.INDENT_OUTPUT)
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public byte[] generate(ReportData data) {
        try {
            byte[] bytes = mapper.writeValueAsBytes(data);
            log.debug("Generated JSON report, size={} bytes, scanId={}", bytes.length, data.getScanId());
            return bytes;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate JSON report: " + e.getMessage(), e);
        }
    }
}