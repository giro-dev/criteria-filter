package dev.agiro.criteriafilter.exception;

import dev.agiro.criteriafilter.model.AggregateFunction;

/**
 * An aggregation applies a function that is not compatible with the target
 * field's type (e.g. {@code SUM} on a {@code String}).
 */
public class UnsupportedAggregationException extends FilterException {

    private final String field;
    private final AggregateFunction function;

    public UnsupportedAggregationException(String field, AggregateFunction function, String reason) {
        super("Aggregate function '" + function + "' is not supported for field '" + field + "': " + reason);
        this.field = field;
        this.function = function;
    }

    public String field() {
        return field;
    }

    public AggregateFunction function() {
        return function;
    }
}
