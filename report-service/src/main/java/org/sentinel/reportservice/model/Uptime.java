package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record Uptime(
        @JacksonXmlProperty(isAttribute = true) Long seconds,
        @JacksonXmlProperty(localName = "lastboot", isAttribute = true) String lastBoot
) {
}
