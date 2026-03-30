package org.sentinel.reportservice.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Scan not found in scan-service (404 forwarded) */
    @ExceptionHandler(ScanNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(ScanNotFoundException e) {
        return error(HttpStatus.NOT_FOUND, e.getMessage());
    }

    /** Scan exists but is not FINISHED yet (still running) */
    @ExceptionHandler(ScanNotCompleteException.class)
    public ResponseEntity<Map<String, Object>> handleNotComplete(ScanNotCompleteException e) {
        return error(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Bad format parameter */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return error(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /** scan-service returned 403 - user does not own the scan */
    @ExceptionHandler(HttpClientErrorException.Forbidden.class)
    public ResponseEntity<Map<String, Object>> handleForbidden(HttpClientErrorException.Forbidden e) {
        return error(HttpStatus.FORBIDDEN, "Access denied — you do not own this scan");
    }

    /** Catch-all */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneral(Exception e) {
        log.error("Unhandled error: {}", e.getMessage(), e);
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<Map<String, Object>> error(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of(
                "status",    status.value(),
                "error",     status.getReasonPhrase(),
                "message",   message,
                "timestamp", Instant.now().toString()
        ));
    }
}