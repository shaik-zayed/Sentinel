package org.sentinel.authservice.exception;

/**
 * Thrown when a token blacklisting operation fails.
 */
public class TokenBlacklistException extends RuntimeException {
    public TokenBlacklistException(String message) {
        super(message);
    }

    public TokenBlacklistException(String message, Throwable cause) {
        super(message, cause);
    }
}
