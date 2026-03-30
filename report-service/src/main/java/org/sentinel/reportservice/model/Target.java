package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder
@Jacksonized
public record Target(
        @JacksonXmlProperty(isAttribute = true) String specification,
        @JacksonXmlProperty(isAttribute = true) String status,
        @JacksonXmlProperty(isAttribute = true) String reason
) {}