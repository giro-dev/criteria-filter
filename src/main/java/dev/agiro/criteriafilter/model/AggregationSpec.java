package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotNull;

import java.util.Locale;

/**
 * One aggregate column of an {@link AggregationRequest}.
 *
 * @param field    logical field name; optional for {@link AggregateFunction#COUNT}
 *                 (counts rows), required otherwise
 * @param function aggregate function to apply
 * @param alias    key of the result in each row; defaults to
 *                 {@code <function>_<field>} (e.g. {@code sum_price}), or
 *                 {@code count} for a field-less {@code COUNT}
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AggregationSpec(
        String field,
        @NotNull AggregateFunction function,
        String alias
) {

    public static AggregationSpec of(AggregateFunction function, String field, String alias) {
        return new AggregationSpec(field, function, alias);
    }

    public static AggregationSpec count(String alias) {
        return new AggregationSpec(null, AggregateFunction.COUNT, alias);
    }

    /** Alias used as result key: the explicit alias, or a name derived from function and field. */
    public String resolvedAlias() {
        if (alias != null && !alias.isBlank()) {
            return alias;
        }
        if (function == null) {
            return null;
        }
        String name = function.name().toLowerCase(Locale.ROOT);
        return field == null || field.isBlank() ? name : name + "_" + field;
    }
}
