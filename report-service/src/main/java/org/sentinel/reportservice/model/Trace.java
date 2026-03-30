package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@Builder
@Jacksonized
public record Trace(
        @JacksonXmlProperty(isAttribute = true) String proto,
        @JacksonXmlProperty(isAttribute = true) Integer port,

        @JacksonXmlProperty(localName = "hop")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Hop> hop
) {}
