package dev.agiro.criteriafilter.exception;

/**
 * Raised when pagination parameters are out of range. Extends
 * {@link IllegalArgumentException} for backwards compatibility.
 */
public class InvalidPageRequestException extends IllegalArgumentException {

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
