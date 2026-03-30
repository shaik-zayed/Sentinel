package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record TaskEnd(
        @JacksonXmlProperty(isAttribute = true) String task,
        @JacksonXmlProperty(isAttribute = true) Long time,
        @JacksonXmlProperty(localName = "extrainfo", isAttribute = true) String extraInfo
) {
}
