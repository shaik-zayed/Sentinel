package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HostnameType {
    @JsonProperty("user") USER,
    @JsonProperty("PTR") PTR
}