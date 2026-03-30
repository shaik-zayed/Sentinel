package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder(toBuilder = true)
@Jacksonized
public record Service(
        @JacksonXmlProperty(isAttribute = true) @NonNull String name,
        @JacksonXmlProperty(isAttribute = true) @NonNull ServiceMethod method,
        @JacksonXmlProperty(isAttribute = true) int conf,
        @JacksonXmlProperty(isAttribute = true) String product,
        @JacksonXmlProperty(isAttribute = true) String version,
        @JacksonXmlProperty(localName = "extrainfo", isAttribute = true) String extraInfo,
        @JacksonXmlProperty(localName = "ostype", isAttribute = true) String osType,
        @JacksonXmlProperty(isAttribute = true) ServiceTunnel tunnel,
        @JacksonXmlProperty(isAttribute = true) ServiceProto proto,
        @JacksonXmlProperty(localName = "rpcnum", isAttribute = true) Integer rpcNum,
        @JacksonXmlProperty(localName = "lowver", isAttribute = true) Integer lowVer,
        @JacksonXmlProperty(localName = "highver", isAttribute = true) Integer highVer,
        @JacksonXmlProperty(isAttribute = true) String hostname,
        @JacksonXmlProperty(localName = "devicetype", isAttribute = true) String deviceType,
        @JacksonXmlProperty(localName = "servicefp", isAttribute = true) String serviceFp,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular List<String> cpes
) {
}
