package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@Builder
@Jacksonized
public record Ports(
        @JacksonXmlProperty(localName = "extraports")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<ExtraPorts> extraPorts,

        @JacksonXmlProperty(localName = "port")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Port> port
) {
}
