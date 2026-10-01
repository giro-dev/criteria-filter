package dev.agiro.criteriafilter.validation;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.exception.UnknownFieldException;
import dev.agiro.criteriafilter.exception.UnsupportedAggregationException;
import dev.agiro.criteriafilter.exception.UnsupportedOperatorException;
import dev.agiro.criteriafilter.metamodel.DatePatternResolver;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadataBuilder;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.AggregateFunction;
import dev.agiro.criteriafilter.model.AggregationRequest;
import dev.agiro.criteriafilter.model.AggregationSpec;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.sample.Measurement;
import dev.agiro.criteriafilter.sample.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static dev.agiro.criteriafilter.model.AggregateFunction.AVG;
import static dev.agiro.criteriafilter.model.AggregateFunction.COUNT;
import static dev.agiro.criteriafilter.model.AggregateFunction.COUNT_DISTINCT;
import static dev.agiro.criteriafilter.model.AggregateFunction.MAX;
import static dev.agiro.criteriafilter.model.AggregateFunction.MIN;
import static dev.agiro.criteriafilter.model.AggregateFunction.SUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AggregationValidationTest {

    private FilterValidator validator;

    @BeforeEach
    void setUp() {
        var builder = new EntityFilterMetadataBuilder(
                new DatePatternResolver(List.of(), "yyyy-MM-dd'T'HH:mm:ss"));
        var registry = new FilterMetadataRegistry();
        registry.initialize(Map.of(
                Product.class, builder.build(Product.class),
                Measurement.class, builder.build(Measurement.class)));
        validator = new FilterValidator(registry, 32, 1000, 1000, 3, 4);
    }

    private static AggregationRequest request(List<String> groupBy, AggregationSpec... specs) {
        return new AggregationRequest(null, groupBy, List.of(specs));
    }

    private static AggregationSpec spec(String field, AggregateFunction function) {
        return new AggregationSpec(field, function, null);
    }

    @Test
    void acceptsEveryFunctionOnCompatibleFields() {
        AggregationRequest request = request(List.of("category", "active"),
                spec("price", SUM), spec("price", AVG), spec("createdAt", MIN), spec("name", MAX));
        assertThatCode(() -> validator.validateAggregation(request, Product.class)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateAggregation(request(List.of(), spec(null, COUNT),
                spec("category", COUNT_DISTINCT), spec("name", COUNT)), Product.class))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsPrimitiveAndBoxedNumericFields() {
        assertThatCode(() -> validator.validateAggregation(request(List.of("product"),
                spec("count", SUM), spec("reading", AVG), spec("day", MIN), spec("product", COUNT_DISTINCT)),
                Measurement.class)).doesNotThrowAnyException();
    }

    @Test
    void filterIsOptionalAndDefaultsToMatchAll() {
        AggregationRequest request = request(null, spec(null, COUNT));
        assertThat(request.groupBy()).isEmpty();
        assertThat(request.filter()).isNotNull();
        assertThatCode(() -> validator.validateAggregation(request, Product.class)).doesNotThrowAnyException();
    }

    @Test
    void validatesFilterExactlyLikeSearch() {
        AggregationRequest unknown = new AggregationRequest(
                new FilterCondition("missing", Operator.EQ, "x", null), List.of(), List.of(spec(null, COUNT)));
        assertThatThrownBy(() -> validator.validateAggregation(unknown, Product.class))
                .isInstanceOf(UnknownFieldException.class);

        AggregationRequest badOperator = new AggregationRequest(
                new FilterCondition("active", Operator.GT, true, null), List.of(), List.of(spec(null, COUNT)));
        assertThatThrownBy(() -> validator.validateAggregation(badOperator, Product.class))
                .isInstanceOf(UnsupportedOperatorException.class);
    }

    @Test
    void rejectsUnknownGroupByField() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of("missing"), spec(null, COUNT)), Product.class))
                .isInstanceOf(UnknownFieldException.class)
                .satisfies(e -> assertThat(((UnknownFieldException) e).field()).isEqualTo("missing"));
    }

    @Test
    void rejectsNonFilterableFieldAsGroupByOrAggregate() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of("internalNote"), spec(null, COUNT)),
                Product.class)).isInstanceOf(UnknownFieldException.class);
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec("internalNote", COUNT)),
                Product.class)).isInstanceOf(UnknownFieldException.class);
    }

    @Test
    void rejectsUnknownAggregationField() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec("missing", SUM)), Product.class))
                .isInstanceOf(UnknownFieldException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "name, SUM", "name, AVG", "category, SUM", "active, AVG", "createdAt, SUM"
    })
    void rejectsSumAndAvgOnNonNumericFields(String field, AggregateFunction function) {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec(field, function)), Product.class))
                .isInstanceOf(UnsupportedAggregationException.class)
                .hasMessageContaining("numeric")
                .satisfies(e -> assertThat(((UnsupportedAggregationException) e).field()).isEqualTo(field));
    }

    @Test
    void rejectsMinMaxOnNonComparableFields() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec("product", MIN)), Measurement.class))
                .isInstanceOf(UnsupportedAggregationException.class)
                .hasMessageContaining("comparable");
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec("product", MAX)), Measurement.class))
                .isInstanceOf(UnsupportedAggregationException.class);
    }

    @ParameterizedTest
    @CsvSource({"SUM", "AVG", "MIN", "MAX", "COUNT_DISTINCT"})
    void requiresFieldForEveryFunctionButCount(AggregateFunction function) {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec(null, function)), Product.class))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("requires a field");
    }

    @Test
    void requiresAtLeastOneAggregationAndAFunction() {
        assertThatThrownBy(() -> validator.validateAggregation(new AggregationRequest(null, List.of("category"), null),
                Product.class)).isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validator.validateAggregation(new AggregationRequest(null, List.of("category"), List.of()),
                Product.class)).isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec("price", null)), Product.class))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("function");
        assertThatThrownBy(() -> validator.validateAggregation(new AggregationRequest(null, List.of(),
                Collections.singletonList(null)), Product.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validator.validateAggregation((AggregationRequest) null, Product.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void rejectsDuplicateOrClashingAliases() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(),
                new AggregationSpec("price", SUM, "x"), new AggregationSpec("price", MAX, "x")), Product.class))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("not unique");
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(),
                spec(null, COUNT), spec(null, COUNT)), Product.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of("category"),
                new AggregationSpec(null, COUNT, "category")), Product.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(),
                new AggregationSpec(null, COUNT, "a".repeat(FilterValidator.MAX_ALIAS_LENGTH + 1))), Product.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void rejectsDuplicateOrBlankGroupBy() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of("category", "category"), spec(null, COUNT)),
                Product.class)).isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validator.validateAggregation(new AggregationRequest(null, List.of(" "),
                List.of(spec(null, COUNT))), Product.class)).isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void enforcesGroupByAndAggregationLimits() {
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of("category", "active", "name", "price"),
                spec(null, COUNT)), Product.class))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("groupBy");
        assertThatThrownBy(() -> validator.validateAggregation(request(List.of(), spec(null, COUNT), spec("price", SUM),
                spec("price", AVG), spec("price", MIN), spec("price", MAX)), Product.class))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("aggregations");
    }

    @Test
    void derivesDefaultAliases() {
        assertThat(spec("price", SUM).resolvedAlias()).isEqualTo("sum_price");
        assertThat(spec(null, COUNT).resolvedAlias()).isEqualTo("count");
        assertThat(spec("category", COUNT_DISTINCT).resolvedAlias()).isEqualTo("count_distinct_category");
        assertThat(new AggregationSpec("price", SUM, "total").resolvedAlias()).isEqualTo("total");
    }
}
