package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder(toBuilder = true)
@Jacksonized
public record Times(
        @JacksonXmlProperty(isAttribute = true) @NonNull String srtt,
        @JacksonXmlProperty(isAttribute = true) @NonNull String rttvar,
        @JacksonXmlProperty(isAttribute = true) @NonNull String to
) {
}