package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum PortProtocol {
    @JsonProperty("ip") IP,
    @JsonProperty("tcp") TCP,
    @JsonProperty("udp") UDP,
    @JsonProperty("sctp") SCTP
}
