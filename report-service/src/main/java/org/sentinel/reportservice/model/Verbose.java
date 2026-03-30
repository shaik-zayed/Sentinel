package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record Verbose(
        @JacksonXmlProperty(isAttribute = true) Integer level
) {
}