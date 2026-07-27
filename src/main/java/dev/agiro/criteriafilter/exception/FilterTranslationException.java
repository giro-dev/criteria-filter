package dev.agiro.criteriafilter.exception;

/**
 * A syntactically valid filter cannot be translated into a backend query
 * (e.g. malformed operand, wrong operand count for the operator).
 */
public class FilterTranslationException extends FilterException {

    public FilterTranslationException(String message) {
        super(message);
    }

    public FilterTranslationException(String message, Throwable cause) {
        super(message, cause);
    }
}
