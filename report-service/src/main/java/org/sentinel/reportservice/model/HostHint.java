package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder(toBuilder = true)
@Jacksonized
public record HostHint(
        @NonNull Status status,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular @NonNull List<Address> addresses,

        Hostnames hostnames
) {
}
