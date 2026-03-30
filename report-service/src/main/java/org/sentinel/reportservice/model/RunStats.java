package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record RunStats(
        @JacksonXmlProperty(localName = "finished") Finished finished,
        @JacksonXmlProperty(localName = "hosts") Hosts hosts
) {}
