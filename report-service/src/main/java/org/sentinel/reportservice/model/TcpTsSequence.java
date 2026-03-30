package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;

@Builder(toBuilder = true)
@Jacksonized
public record TcpTsSequence(
        //class keyword cannot be used.
        @JacksonXmlProperty(isAttribute = true, localName = "class") @NonNull String clazz,
        @JacksonXmlProperty(isAttribute = true) String values
) {
}