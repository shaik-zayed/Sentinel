package org.sentinel.reportservice.model;


import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record PortUsed(
        @JacksonXmlProperty(isAttribute = true) @NonNull String state,
        @JacksonXmlProperty(isAttribute = true) @NonNull PortProtocol proto,
        @JacksonXmlProperty(localName = "portid", isAttribute = true) int portId
) {
}