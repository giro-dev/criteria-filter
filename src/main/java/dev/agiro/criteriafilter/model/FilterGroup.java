package dev.agiro.criteriafilter.model;

import java.util.List;

/**
 * Combines child {@link FilterNode}s with a {@link LogicalOperator}.
 */
public record FilterGroup(
        LogicalOperator combinator,
        List<FilterNode> filters
) implements FilterNode {

    public FilterGroup {
        filters = filters == null ? List.of() : List.copyOf(filters);
    }
}
