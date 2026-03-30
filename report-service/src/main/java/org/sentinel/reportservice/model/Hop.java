package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder(toBuilder = true)
@Jacksonized
public record Hop(
        @JacksonXmlProperty(isAttribute = true) @NonNull String ttl,
        @JacksonXmlProperty(isAttribute = true) String rtt,
        @JacksonXmlProperty(isAttribute = true) String ipaddr,
        @JacksonXmlProperty(isAttribute = true) String host
) {
}
