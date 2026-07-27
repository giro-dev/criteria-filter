package dev.agiro.criteriafilter.exception;

import dev.agiro.criteriafilter.model.Operator;

/**
 * A filter uses an operator that is not allowed for the target field.
 */
public class UnsupportedOperatorException extends FilterException {

    private final String field;
    private final Operator operator;

    public UnsupportedOperatorException(String field, Operator operator) {
        super("Operator '" + operator + "' is not supported for field '" + field + "'");
        this.field = field;
        this.operator = operator;
    }

    public String field() {
        return field;
    }

    public Operator operator() {
        return operator;
    }
}
