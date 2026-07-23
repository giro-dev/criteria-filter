package dev.agiro.criteriafilter.validation;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.exception.UnsupportedOperatorException;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.Operator;

import java.util.List;

/**
 * Validates a {@link FilterRequest} against the entity metamodel before it
 * reaches any backend, so a {@code 400 Bad Request} is identical regardless of
 * backend. Lives in the controller/service layer, not inside a backend.
 */
public class FilterValidator {

    private final FilterMetadataRegistry registry;

    public FilterValidator(FilterMetadataRegistry registry) {
        this.registry = registry;
    }

    public void validate(FilterRequest request, Class<?> entityType) {
        EntityFilterMetadata metadata = registry.require(entityType);
        if (request == null || request.filter() == null) {
            throw new FilterTranslationException("Filter request must contain a filter node");
        }
        validateNode(request.filter(), metadata);
    }

    private void validateNode(FilterNode node, EntityFilterMetadata metadata) {
        if (node instanceof FilterGroup group) {
            if (group.combinator() == null) {
                throw new FilterTranslationException("Filter group is missing a combinator");
            }
            for (FilterNode child : group.filters()) {
                validateNode(child, metadata);
            }
        } else if (node instanceof FilterCondition condition) {
            validateCondition(condition, metadata);
        } else {
            throw new FilterTranslationException("Unsupported filter node: " + node);
        }
    }

    private void validateCondition(FilterCondition condition, EntityFilterMetadata metadata) {
        if (condition.operator() == null) {
            throw new FilterTranslationException(
                    "Condition on field '" + condition.field() + "' is missing an operator");
        }
        FieldMetadata field = metadata.require(condition.field()); // throws UnknownFieldException
        if (!field.supports(condition.operator())) {
            throw new UnsupportedOperatorException(condition.field(), condition.operator());
        }
        validateArity(condition, field);
    }

    private void validateArity(FilterCondition condition, FieldMetadata field) {
        Operator operator = condition.operator();
        List<Object> operands = condition.operands();
        switch (operator.arity()) {
            case NONE -> require(operands.isEmpty(), field, operator, "no value");
            case SINGLE -> require(operands.size() == 1, field, operator, "exactly 1 value");
            case PAIR -> require(operands.size() == 2, field, operator, "exactly 2 values");
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
