package dev.agiro.criteriafilter.validation;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.exception.UnknownFieldException;
import dev.agiro.criteriafilter.exception.UnsupportedOperatorException;
import dev.agiro.criteriafilter.metamodel.DatePatternResolver;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadataBuilder;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.LogicalOperator;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.sample.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterValidatorTest {

    private FilterValidator validator;

    @BeforeEach
    void setUp() {
        var builder = new EntityFilterMetadataBuilder(
                new DatePatternResolver(List.of(), "yyyy-MM-dd'T'HH:mm:ss"));
        var registry = new FilterMetadataRegistry();
        registry.initialize(Map.of(Product.class, builder.build(Product.class)));
        validator = new FilterValidator(registry);
    }

    @Test
    void acceptsValidNestedRequest() {
        FilterRequest request = new FilterRequest(new FilterGroup(LogicalOperator.AND, List.of(
                new FilterCondition("name", Operator.LIKE, "book", null),
                new FilterCondition("price", Operator.BETWEEN, null, List.of(10, 20))
        )));
        assertThatCode(() -> validator.validate(request, Product.class)).doesNotThrowAnyException();
    }

    @Test
    void rejectsUnknownField() {
        FilterRequest request = new FilterRequest(
                new FilterCondition("missing", Operator.EQ, "x", null));
        assertThatThrownBy(() -> validator.validate(request, Product.class))
                .isInstanceOf(UnknownFieldException.class);
    }

    @Test
    void rejectsUnsupportedOperatorForField() {
        // active only allows EQ.
        FilterRequest request = new FilterRequest(
                new FilterCondition("active", Operator.GT, true, null));
        assertThatThrownBy(() -> validator.validate(request, Product.class))
                .isInstanceOf(UnsupportedOperatorException.class);
    }

    @Test
    void rejectsWrongArity() {
        FilterRequest request = new FilterRequest(
                new FilterCondition("price", Operator.BETWEEN, null, List.of(10)));
        assertThatThrownBy(() -> validator.validate(request, Product.class))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("exactly 2 values");
    }
}
