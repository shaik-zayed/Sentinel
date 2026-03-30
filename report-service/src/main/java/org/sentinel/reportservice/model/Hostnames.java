package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@Builder
@Jacksonized
public record Hostnames(
        @JacksonXmlProperty(localName = "hostname")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Hostname> hostname
) {
}