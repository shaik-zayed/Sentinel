package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder(toBuilder = true)
@Jacksonized
public record Hosts(
        @JacksonXmlProperty(isAttribute = true) int up,
        @JacksonXmlProperty(isAttribute = true) int down,
        @JacksonXmlProperty(isAttribute = true) int total
) {
    public Hosts {
        if (up == 0) up = 0;  // Explicit but handled
        if (down == 0) down = 0;
    }
}