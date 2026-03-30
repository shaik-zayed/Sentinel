package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record Output(
        @JacksonXmlProperty(isAttribute = true) OutputType type,
        @JacksonXmlText String content
) {
}
