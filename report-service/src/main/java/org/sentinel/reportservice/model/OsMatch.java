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
public record OsMatch(
        @JacksonXmlProperty(isAttribute = true) @NonNull String name,
        @JacksonXmlProperty(isAttribute = true) int accuracy,
        @JacksonXmlProperty(isAttribute = true) int line,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("osclass") List<OsClass> osClasses
) {
}
