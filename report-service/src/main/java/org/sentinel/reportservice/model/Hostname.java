package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record Hostname(
        @JacksonXmlProperty(isAttribute = true) String name,
        @JacksonXmlProperty(isAttribute = true) String type
) {
}
