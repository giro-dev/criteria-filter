package dev.agiro.criteriafilter.validation;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.exception.UnsupportedAggregationException;
import dev.agiro.criteriafilter.exception.UnsupportedOperatorException;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.AggregateFunction;
import dev.agiro.criteriafilter.model.AggregationRequest;
import dev.agiro.criteriafilter.model.AggregationSpec;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.Operator;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Validates a {@link FilterRequest} against the entity metamodel before it
 * reaches any backend, so a {@code 400 Bad Request} is identical regardless of
 * backend. Lives in the controller/service layer, not inside a backend.
 */
public class FilterValidator {

    public static final int DEFAULT_MAX_DEPTH = 32;
    public static final int DEFAULT_MAX_CONDITIONS = 1000;
    public static final int DEFAULT_MAX_VALUES = 1000;
    public static final int DEFAULT_MAX_GROUP_BY = 16;
    public static final int DEFAULT_MAX_AGGREGATIONS = 64;
    public static final int MAX_ALIAS_LENGTH = 128;

    private final FilterMetadataRegistry registry;
    private final int maxDepth;
    private final int maxConditions;
    private final int maxValues;
    private final int maxGroupBy;
    private final int maxAggregations;

    public FilterValidator(FilterMetadataRegistry registry) {
        this(registry, DEFAULT_MAX_DEPTH, DEFAULT_MAX_CONDITIONS, DEFAULT_MAX_VALUES);
    }

    /**
     * @param maxDepth      maximum group nesting depth (a single condition has depth 1)
     * @param maxConditions maximum number of conditions in the whole tree
     * @param maxValues     maximum number of operands in a single condition
     */
    public FilterValidator(FilterMetadataRegistry registry, int maxDepth, int maxConditions, int maxValues) {
        this(registry, maxDepth, maxConditions, maxValues, DEFAULT_MAX_GROUP_BY, DEFAULT_MAX_AGGREGATIONS);
    }

    /**
     * @param maxGroupBy      maximum number of {@code groupBy} fields in an aggregation
     * @param maxAggregations maximum number of aggregate columns in an aggregation
     */
    public FilterValidator(FilterMetadataRegistry registry, int maxDepth, int maxConditions, int maxValues,
                           int maxGroupBy, int maxAggregations) {
        this.registry = registry;
        this.maxDepth = maxDepth;
        this.maxConditions = maxConditions;
        this.maxValues = maxValues;
        this.maxGroupBy = maxGroupBy;
        this.maxAggregations = maxAggregations;
    }

    public void validate(FilterRequest request, Class<?> entityType) {
        EntityFilterMetadata metadata = registry.require(entityType);
        if (request == null || request.filter() == null) {
            throw new FilterTranslationException("Filter request must contain a filter node");
        }
        validateNode(request.filter(), metadata, 1, new int[1]);
    }

    /**
     * Validates an {@link AggregationRequest}: the filter exactly like
     * {@link #validate(FilterRequest, Class)}, then group-by fields, aggregate
     * fields, function/type compatibility and result aliases.
     */
    public void validateAggregation(AggregationRequest request, Class<?> entityType) {
        EntityFilterMetadata metadata = registry.require(entityType);
        if (request == null) {
            throw new FilterTranslationException("Aggregation request must not be null");
        }
        validateNode(request.filter(), metadata, 1, new int[1]);

        List<String> groupBy = request.groupBy();
        if (groupBy.size() > maxGroupBy) {
            throw new FilterTranslationException(
                    "Aggregation exceeds the maximum of " + maxGroupBy + " groupBy fields");
        }
        Set<String> keys = new HashSet<>();
        for (String field : groupBy) {
            if (field == null || field.isBlank()) {
                throw new FilterTranslationException("groupBy contains an empty field name");
            }
            metadata.require(field); // throws UnknownFieldException
            if (!keys.add(field)) {
                throw new FilterTranslationException("groupBy field '" + field + "' is listed more than once");
            }
        }

        List<AggregationSpec> aggregations = request.aggregations();
        if (aggregations == null || aggregations.isEmpty()) {
            throw new FilterTranslationException("Aggregation request must contain at least one aggregation");
        }
        if (aggregations.size() > maxAggregations) {
            throw new FilterTranslationException(
                    "Aggregation exceeds the maximum of " + maxAggregations + " aggregations");
        }
        for (AggregationSpec spec : aggregations) {
            validateSpec(spec, metadata);
            String alias = spec.resolvedAlias();
            if (alias.length() > MAX_ALIAS_LENGTH || alias.indexOf('\u0000') >= 0) {
                throw new FilterTranslationException("Aggregation alias must be at most "
                        + MAX_ALIAS_LENGTH + " characters and contain no NUL character");
            }
            if (!keys.add(alias)) {
                throw new FilterTranslationException("Aggregation alias '" + alias
                        + "' is not unique (it clashes with another alias or a groupBy field)");
            }
        }
    }

    private void validateSpec(AggregationSpec spec, EntityFilterMetadata metadata) {
        if (spec == null) {
            throw new FilterTranslationException("Aggregation must not be null");
        }
        AggregateFunction function = spec.function();
        if (function == null) {
            throw new FilterTranslationException("Aggregation is missing a function");
        }
        if (spec.field() == null || spec.field().isBlank()) {
            if (!function.fieldOptional()) {
                throw new FilterTranslationException("Aggregate function '" + function + "' requires a field");
            }
            return;
        }
        FieldMetadata field = metadata.require(spec.field()); // throws UnknownFieldException
        Class<?> type = boxed(field.type());
        switch (function) {
            case SUM, AVG -> {
                if (!Number.class.isAssignableFrom(type)) {
                    throw new UnsupportedAggregationException(spec.field(), function,
                            "requires a numeric field, but the field is " + type.getSimpleName());
                }
            }
            case MIN, MAX -> {
                if (!Comparable.class.isAssignableFrom(type)) {
                    throw new UnsupportedAggregationException(spec.field(), function,
                            "requires a comparable field, but the field is " + type.getSimpleName());
                }
            }
            case COUNT, COUNT_DISTINCT -> {
                // any type
            }
        }
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return java.lang.invoke.MethodType.methodType(type).wrap().returnType();
    }

    private void validateNode(FilterNode node, EntityFilterMetadata metadata, int depth, int[] conditions) {
        if (node == null) {
            throw new FilterTranslationException("Filter node must not be null");
        }
        if (depth > maxDepth) {
            throw new FilterTranslationException("Filter exceeds the maximum nesting depth of " + maxDepth);
        }
        if (node instanceof FilterGroup group) {
            if (group.combinator() == null) {
                throw new FilterTranslationException("Filter group is missing a combinator");
            }
            for (FilterNode child : group.filters()) {
                validateNode(child, metadata, depth + 1, conditions);
            }
        } else if (node instanceof FilterCondition condition) {
            if (++conditions[0] > maxConditions) {
                throw new FilterTranslationException(
                        "Filter exceeds the maximum of " + maxConditions + " conditions");
            }
            validateCondition(condition, metadata);
        } else {
            throw new FilterTranslationException("Unsupported filter node: " + node);
        }
    }

    private void validateCondition(FilterCondition condition, EntityFilterMetadata metadata) {
        if (condition.field() == null || condition.field().isBlank()) {
            throw new FilterTranslationException("Condition is missing a field");
        }
        if (condition.operator() == null) {
            throw new FilterTranslationException(
                    "Condition on field '" + condition.field() + "' is missing an operator");
        }
        FieldMetadata field = metadata.require(condition.field()); // throws UnknownFieldException
        if (!field.supports(condition.operator())) {
            throw new UnsupportedOperatorException(condition.field(), condition.operator());
        }
        if (condition.value() != null && condition.values() != null) {
            throw new FilterTranslationException("Condition on field '" + condition.field()
                    + "' must use either 'value' or 'values', not both");
        }
        validateArity(condition, field);
        List<Object> operands = condition.operands();
        if (operands.size() > maxValues) {
            throw new FilterTranslationException("Condition on field '" + condition.field()
                    + "' exceeds the maximum of " + maxValues + " values");
        }
        if (operands.stream().anyMatch(Objects::isNull)) {
            throw new FilterTranslationException("Condition on field '" + condition.field()
                    + "' contains a null value; use IS_NULL / IS_NOT_NULL to match nulls");
        }
        if (operands.stream().anyMatch(o -> o instanceof String s && s.indexOf('\u0000') >= 0)) {
            throw new FilterTranslationException("Condition on field '" + condition.field()
                    + "' contains a NUL character");
        }
    }

    private void validateArity(FilterCondition condition, FieldMetadata field) {
        Operator operator = condition.operator();
        List<Object> operands = condition.operands();
        switch (operator.arity()) {
            case NONE -> require(operands.isEmpty(), field, operator, "no value");
            case SINGLE -> require(operands.size() == 1, field, operator, "exactly 1 value");
            case PAIR -> require(operands.size() == 2, field, operator, "exactly 2 values");
            case SINGLE_OR_PAIR -> require(operands.size() == 1 || operands.size() == 2,
                    field, operator, "1 or 2 values");
            case MULTI -> require(!operands.isEmpty(), field, operator, "at least 1 value");
        }
    }

    private void require(boolean condition, FieldMetadata field, Operator operator, String expectation) {
        if (!condition) {
            throw new FilterTranslationException("Operator '" + operator + "' on field '"
                    + field.logicalName() + "' requires " + expectation);
        }
    }
}
