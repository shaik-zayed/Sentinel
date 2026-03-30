package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record TaskBegin(
        @JacksonXmlProperty(isAttribute = true) String task,
        @JacksonXmlProperty(isAttribute = true) Long time,
        @JacksonXmlProperty(localName = "extrainfo", isAttribute = true) String extraInfo
) {
}
