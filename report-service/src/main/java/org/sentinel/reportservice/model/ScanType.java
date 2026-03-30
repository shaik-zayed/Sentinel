package org.sentinel.reportservice.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ScanType {
    @JsonProperty("syn") SYN,
    @JsonProperty("ack") ACK,
    @JsonProperty("bounce") BOUNCE,
    @JsonProperty("connect") CONNECT,
    @JsonProperty("null") NULL,
    @JsonProperty("xmas") XMAS,
    @JsonProperty("window") WINDOW,
    @JsonProperty("maimon") MAIMON,
    @JsonProperty("fin") FIN,
    @JsonProperty("udp") UDP,
    @JsonProperty("sctpinit") SCTP_INIT,
    @JsonProperty("sctpcookieecho") SCTP_COOKIE_ECHO,
    @JsonProperty("ipproto") IP_PROTO
}
