package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ExitStatus {
    @JsonProperty("error") ERROR,
    @JsonProperty("success") SUCCESS
}