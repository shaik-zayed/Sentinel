package org.sentinel.nmapservice.exception;

/**
 * Thrown when a Docker container operation fails (creation, execution, log
 * retrieval).
 */
public class ContainerExecutionException extends RuntimeException {
    public ContainerExecutionException(String message) {
        super(message);
    }

    public ContainerExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
