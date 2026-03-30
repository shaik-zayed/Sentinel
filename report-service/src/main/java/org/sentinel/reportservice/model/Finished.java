package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@Builder(toBuilder = true)
@Jacksonized
public record Finished(
        @JacksonXmlProperty(isAttribute = true) @NonNull String time,
        @JacksonXmlProperty(localName = "timestr", isAttribute = true) String timeString,
        @JacksonXmlProperty(isAttribute = true) @NonNull String elapsed,
        @JacksonXmlProperty(isAttribute = true) String summary,
        @JacksonXmlProperty(isAttribute = true) ExitStatus exit,
        @JacksonXmlProperty(localName = "errormsg", isAttribute = true) String errorMsg
) {
}