package dev.agiro.criteriafilter.exception;

/**
 * A filter references a field that is not exposed by the entity metadata.
 */
public class UnknownFieldException extends FilterException {

    private final String field;

    public UnknownFieldException(String field) {
        super("Unknown filter field: '" + field + "'");
        this.field = field;
    }

    public String field() {
        return field;
    }
}
