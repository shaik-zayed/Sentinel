package org.sentinel.reportservice.model;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

import java.util.List;

@Builder
@Jacksonized
@JacksonXmlRootElement(localName = "nmaprun")
public record NmapRun(
        @JacksonXmlProperty(isAttribute = true) @NonNull String scanner,
        @JacksonXmlProperty(isAttribute = true) String args,
        @JacksonXmlProperty(isAttribute = true) String start,
        @JacksonXmlProperty(localName = "startstr", isAttribute = true) String startStr,
        @JacksonXmlProperty(isAttribute = true) @NonNull String version,
        @JacksonXmlProperty(isAttribute = true) String profile_name,
        @JacksonXmlProperty(localName = "xmloutputversion", isAttribute = true) @NonNull String xmlOutputVersion,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("scaninfo") List<ScanInfo> scanInfo,
        @NonNull Verbose verbose,
        @NonNull Debugging debugging,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("taskbegin") List<TaskBegin> taskBegin,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("taskprogress") List<TaskProgress> taskProgress,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("taskend") List<TaskEnd> taskEnd,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("hosthint") List<HostHint> hostHint,

        Prescript prescript,

        @JacksonXmlElementWrapper(useWrapping = false)
        @NonNull @Singular List<Host> hosts,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular List<Target> targets,

        Postscript postscript,

        @JacksonXmlElementWrapper(useWrapping = false)
        @Singular("output") List<Output> output,

        @NonNull RunStats runstats
) {
}
