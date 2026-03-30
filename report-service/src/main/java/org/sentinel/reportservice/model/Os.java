package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
public record Os(
        @JacksonXmlProperty(localName = "portused")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<PortUsed> portUsed,

        @JacksonXmlProperty(localName = "osmatch")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<OsMatch> osMatch,

        @JacksonXmlProperty(localName = "osfingerprint")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<OsFingerprint> osFingerprint
) {
}