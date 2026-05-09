package org.sentinel.scanservice.exception;

public class TooManyRequestsException extends RuntimeException {
    public TooManyRequestsException(String message, Throwable cause) {
        super(message, cause);
    }
}