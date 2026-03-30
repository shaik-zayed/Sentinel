package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record ScanInfo(
        @JacksonXmlProperty(isAttribute = true) @NonNull ScanType type,
        @JacksonXmlProperty(localName = "scanflags", isAttribute = true) String scanFlags,
        @JacksonXmlProperty(isAttribute = true) @NonNull PortProtocol protocol,
        @JacksonXmlProperty(localName = "numservices", isAttribute = true) int numServices,
        @JacksonXmlProperty(isAttribute = true) @NonNull String services
) {}