package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum HostState {
    @JsonProperty("up") UP,
    @JsonProperty("down") DOWN,
    @JsonProperty("unknown") UNKNOWN,
    @JsonProperty("skipped") SKIPPED
}