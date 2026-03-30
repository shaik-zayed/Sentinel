package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record TaskProgress(
        @JacksonXmlProperty(isAttribute = true) String task,
        @JacksonXmlProperty(isAttribute = true) Long time,
        @JacksonXmlProperty(isAttribute = true) Double percent,
        @JacksonXmlProperty(isAttribute = true) Integer remaining,
        @JacksonXmlProperty(isAttribute = true) String etc
) {
}
