package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;

import java.util.List;


@Builder(toBuilder = true)
@Jacksonized
public record Postscript(
        // ⭐ CHANGED: script+ (at least one required)
        @JacksonXmlElementWrapper(useWrapping = false)
        @NonNull @Singular List<Script> scripts
) {
}

