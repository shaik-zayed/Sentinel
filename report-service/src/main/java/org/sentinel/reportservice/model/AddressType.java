package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum AddressType {
    @JsonProperty("ipv4") IPV4,
    @JsonProperty("ipv6") IPV6,
    @JsonProperty("mac") MAC
}