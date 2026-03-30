package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Builder
@Jacksonized
public record ScriptTable(
        @JacksonXmlProperty(isAttribute = true) String key,

        @JacksonXmlProperty(localName = "elem")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<ScriptElement> elem,

        @JacksonXmlProperty(localName = "table")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<ScriptTable> table
) {
}
