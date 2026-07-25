package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A node of a filter tree: either a leaf {@link FilterCondition} or a
 * {@link FilterGroup} combining child nodes.
 *
 * <p>The concrete subtype is deduced by Jackson from the properties present in
 * the JSON payload:
 * <ul>
 *   <li>{@code field}/{@code operator} → {@link FilterCondition}</li>
 *   <li>{@code and}/{@code or} or {@code combinator}/{@code filters} → {@link FilterGroup}</li>
 * </ul>
 *
 * <p>Example nested filter (IHO-style):
 * <pre>{@code
 * {
 *   "and": [
 *     {"field": "status", "operator": "EQ", "value": "ACTIVE"},
 *     {"or": [
 *       {"field": "category", "operator": "EQ", "value": "BOOK"},
 *       {"field": "price", "operator": "LT", "value": 20}
 *     ]}
 *   ]
 * }
 * }</pre>
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(FilterCondition.class),
        @JsonSubTypes.Type(FilterGroup.class)
})
public sealed interface FilterNode permits FilterCondition, FilterGroup {
}
