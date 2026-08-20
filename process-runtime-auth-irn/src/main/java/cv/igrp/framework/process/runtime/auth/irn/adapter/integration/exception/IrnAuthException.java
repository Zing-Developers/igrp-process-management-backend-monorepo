package cv.igrp.framework.process.runtime.auth.irn.adapter.integration.exception;

/**
 * Exception thrown when there is an error communicating with the IRN authentication API.
 */
public class IrnAuthException extends RuntimeException {

    public IrnAuthException(String message) {
        super(message);
    }

    public IrnAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
