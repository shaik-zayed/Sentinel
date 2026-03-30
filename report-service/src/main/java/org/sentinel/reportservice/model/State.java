package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder(toBuilder = true)
@Jacksonized
public record State(
        @JacksonXmlProperty(isAttribute = true) @NonNull String state,
        @JacksonXmlProperty(isAttribute = true) @NonNull String reason,
        @JacksonXmlProperty(isAttribute = true, localName = "reason_ttl") @NonNull String reasonTtl,
        @JacksonXmlProperty(isAttribute = true, localName = "reason_ip") String reasonIp
) {
}
