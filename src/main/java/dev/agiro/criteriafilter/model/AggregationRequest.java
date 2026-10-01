package dev.agiro.criteriafilter.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Grouped aggregation over the entities matching {@link #filter()}.
 *
 * <pre>{@code
 * {
 *   "filter": {"and": [{"field": "active", "operator": "EQ", "value": true}]},
 *   "groupBy": ["category"],
 *   "aggregations": [
 *     {"field": "price", "function": "SUM", "alias": "totalPrice"},
 *     {"function": "COUNT", "alias": "n"}
 *   ]
 * }
 * }</pre>
 *
 * @param filter       same filter tree as {@link FilterRequest#filter()}; optional,
 *                     defaults to an empty {@code AND} group (matches everything)
 * @param groupBy      logical fields to group by, in order; optional (empty means a
 *                     single row aggregating every match)
 * @param aggregations aggregate columns; at least one is required
 */
public record AggregationRequest(
        FilterNode filter,
        List<String> groupBy,
        @NotNull List<@Valid AggregationSpec> aggregations
) {

    public AggregationRequest {
        filter = filter == null ? new FilterGroup(LogicalOperator.AND, List.of()) : filter;
        groupBy = groupBy == null ? List.of() : List.copyOf(groupBy);
    }

    /** The filter part as a {@link FilterRequest}, e.g. for {@code FilterValidator}. */
    public FilterRequest filterRequest() {
        return new FilterRequest(filter);
    }

    /** Copy of this request with a different filter (used after interceptors add conditions). */
    public AggregationRequest withFilter(FilterNode newFilter) {
        return new AggregationRequest(newFilter, groupBy, aggregations);
    }
}
