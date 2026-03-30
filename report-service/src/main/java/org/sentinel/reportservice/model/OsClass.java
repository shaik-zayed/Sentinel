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
public record OsClass(
        @JacksonXmlProperty(isAttribute = true) @NonNull String vendor,
        @JacksonXmlProperty(localName = "osgen", isAttribute = true) String osGen,
        @JacksonXmlProperty(isAttribute = true) String type,
        @JacksonXmlProperty(isAttribute = true) int accuracy,
        @JacksonXmlProperty(localName = "osfamily", isAttribute = true) @NonNull String osFamily,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular List<String> cpes
) {
}
