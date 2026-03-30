package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
public record Hostscript(
        @JacksonXmlProperty(localName = "script")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Script> script
) {
}

