package dev.agiro.criteriafilter.model;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * A node of a filter tree: either a leaf {@link FilterCondition} or a
 * {@link FilterGroup} combining child nodes.
 *
 * <p>The concrete subtype is deduced by Jackson from the properties present in
 * the JSON payload ({@code field}/{@code operator} vs {@code combinator}/{@code filters}),
 * so clients never send an explicit type discriminator.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
@JsonSubTypes({
        @JsonSubTypes.Type(FilterCondition.class),
        @JsonSubTypes.Type(FilterGroup.class)
})
public sealed interface FilterNode permits FilterCondition, FilterGroup {
}
