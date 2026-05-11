package org.sentinel.authservice.exception;

import lombok.Builder;

import java.time.OffsetDateTime;

@Builder
public record ErrorResponse(
        OffsetDateTime timeStamp,
        int status,
        String message,
        String path) {
}
