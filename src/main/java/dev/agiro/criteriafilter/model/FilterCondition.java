package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * A single leaf comparison against a logical field.
 *
 * <p>{@code value} carries the operand for single-operand operators; {@code values}
 * carries operands for {@code IN}/{@code BETWEEN}. Operators with {@link Operator.Arity#NONE}
 * (e.g. {@code IS_NULL}) require neither.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record FilterCondition(
        String field,
        Operator operator,
        Object value,
        List<Object> values
) implements FilterNode {

    /** Operands as a normalized list regardless of which JSON field was populated. */
    public List<Object> operands() {
        if (values != null) {
            return values;
        }
        if (value != null) {
            return List.of(value);
        }
        return List.of();
    }
}
