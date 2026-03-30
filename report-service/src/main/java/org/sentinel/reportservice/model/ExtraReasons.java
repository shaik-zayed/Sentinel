package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record ExtraReasons(
        @JacksonXmlProperty(isAttribute = true) String reason,
        @JacksonXmlProperty(isAttribute = true) Integer count,
        @JacksonXmlProperty(isAttribute = true) String proto,
        @JacksonXmlProperty(isAttribute = true) String ports
) {
}
