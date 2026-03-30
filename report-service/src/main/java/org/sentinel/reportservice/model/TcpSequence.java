package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record TcpSequence(
        @JacksonXmlProperty(isAttribute = true) @NonNull String index,
        @JacksonXmlProperty(isAttribute = true) @NonNull String difficulty,
        @JacksonXmlProperty(isAttribute = true) @NonNull String values
) {
}

