package org.sentinel.reportservice.model;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@Builder
@Jacksonized
public record Host(
        @JacksonXmlProperty(localName = "starttime", isAttribute = true) String startTime,
        @JacksonXmlProperty(isAttribute = true) String endtime,
        @JacksonXmlProperty(isAttribute = true) String comment,

        @JacksonXmlProperty(localName = "status") Status status,

        @JacksonXmlProperty(localName = "address")
        @JacksonXmlElementWrapper(useWrapping = false)
        List<Address> addresses,

        @JacksonXmlProperty(localName = "hostnames") Hostnames hostnames,
        @JacksonXmlProperty(localName = "smurf") Smurf smurf,
        @JacksonXmlProperty(localName = "ports") Ports ports,
        @JacksonXmlProperty(localName = "os") Os os,
        @JacksonXmlProperty(localName = "distance") Distance distance,
        @JacksonXmlProperty(localName = "uptime") Uptime uptime,
        @JacksonXmlProperty(localName = "tcpsequence") TcpSequence tcpSequence,
        @JacksonXmlProperty(localName = "ipidsequence") IpIdSequence ipIdSequence,
        @JacksonXmlProperty(localName = "tcptssequence") TcpTsSequence tcpTsSequence,
        @JacksonXmlProperty(localName = "hostscript") Hostscript hostScript,
        @JacksonXmlProperty(localName = "trace") Trace trace,
        @JacksonXmlProperty(localName = "times") Times times,
        @JacksonXmlProperty(localName = "owner") Owner owner
) {
}
