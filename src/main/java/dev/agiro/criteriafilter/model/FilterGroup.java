package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Combines child {@link FilterNode}s with a {@link LogicalOperator}.
 *
 * <p>Supports two JSON syntaxes:
 * <ul>
 *   <li><b>Explicit combinator:</b> {@code {"combinator": "AND", "filters": [...]}}</li>
 *   <li><b>IHO-style nested:</b> {@code {"and": [...]}} or {@code {"or": [...]}}</li>
 * </ul>
 *
 * <p>Both syntaxes support arbitrary nesting of AND/OR groups and leaf conditions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class FilterGroup implements FilterNode {

    private final LogicalOperator combinator;
    private final List<FilterNode> filters;

    /**
     * Primary constructor for explicit combinator syntax.
     */
    public FilterGroup(LogicalOperator combinator, List<FilterNode> filters) {
        this.combinator = combinator;
        this.filters = filters == null ? List.of() : List.copyOf(filters);
    }

    /**
     * JSON creator supporting both syntaxes.
     */
    @JsonCreator
    public static FilterGroup create(
            @JsonProperty("combinator") LogicalOperator combinator,
            @JsonProperty("filters") List<FilterNode> filters,
            @JsonProperty("and") List<FilterNode> and,
            @JsonProperty("or") List<FilterNode> or) {

        if (and != null) {
            return new FilterGroup(LogicalOperator.AND, and);
        }
        if (or != null) {
            return new FilterGroup(LogicalOperator.OR, or);
        }
        if (combinator != null) {
            return new FilterGroup(combinator, filters);
        }
        // Default to AND with empty filters if nothing specified
        return new FilterGroup(LogicalOperator.AND, filters != null ? filters : List.of());
    }

    public LogicalOperator combinator() {
        return combinator;
    }

    public List<FilterNode> filters() {
        return filters;
    }

    // --- JSON serialization for IHO-style output ---

    @JsonProperty("and")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<FilterNode> getAnd() {
        return combinator == LogicalOperator.AND ? filters : null;
    }

    @JsonProperty("or")
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public List<FilterNode> getOr() {
        return combinator == LogicalOperator.OR ? filters : null;
    }

    // Hide the explicit combinator/filters in JSON output (use and/or instead)
    @JsonIgnore
    public LogicalOperator getCombinator() {
        return combinator;
    }

    @JsonIgnore
    public List<FilterNode> getFilters() {
        return filters;
    }

    // --- Static factory methods for fluent API ---

    public static FilterGroup and(FilterNode... nodes) {
        return new FilterGroup(LogicalOperator.AND, List.of(nodes));
    }

    public static FilterGroup and(List<FilterNode> nodes) {
        return new FilterGroup(LogicalOperator.AND, nodes);
    }

    public static FilterGroup or(FilterNode... nodes) {
        return new FilterGroup(LogicalOperator.OR, List.of(nodes));
    }

    public static FilterGroup or(List<FilterNode> nodes) {
        return new FilterGroup(LogicalOperator.OR, nodes);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FilterGroup that)) return false;
        return combinator == that.combinator && filters.equals(that.filters);
    }

    @Override
    public int hashCode() {
        return 31 * combinator.hashCode() + filters.hashCode();
    }

    @Override
    public String toString() {
        return "FilterGroup[combinator=" + combinator + ", filters=" + filters + "]";
    }
}
