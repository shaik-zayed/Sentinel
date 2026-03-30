package org.sentinel.authservice.exceptions;

/**
 * Thrown when a logout operation fails (e.g., token blacklisting error).
 */
public class LogoutException extends RuntimeException {
    public LogoutException(String message) {
        super(message);
    }

    public LogoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
