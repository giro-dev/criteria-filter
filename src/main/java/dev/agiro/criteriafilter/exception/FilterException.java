package dev.agiro.criteriafilter.exception;

/**
 * Base type for every error raised while validating or translating a filter,
 * so the three backends throw a consistent hierarchy.
 */
public abstract class FilterException extends RuntimeException {

    protected FilterException(String message) {
        super(message);
    }

    protected FilterException(String message, Throwable cause) {
        super(message, cause);
    }
}
