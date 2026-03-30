package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@Builder(toBuilder = true)
@Jacksonized
public record Port(
        @JacksonXmlProperty(isAttribute = true) @NonNull PortProtocol protocol,
        @JacksonXmlProperty(localName = "portid", isAttribute = true) int portId,

        @NonNull State state,
        Owner owner,
        Service service,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular List<Script> scripts
) {}