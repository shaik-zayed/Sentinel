package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ServiceMethod {
    @JsonProperty("table") TABLE,
    @JsonProperty("probed") PROBED
}