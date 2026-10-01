package dev.agiro.criteriafilter.validation;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.exception.UnknownFieldException;
import dev.agiro.criteriafilter.metamodel.DatePatternResolver;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadataBuilder;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.LogicalOperator;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.sample.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilterValidatorHardeningTest {

    private FilterMetadataRegistry registry;
    private FilterValidator validator;

    @BeforeEach
    void setUp() {
        var builder = new EntityFilterMetadataBuilder(
                new DatePatternResolver(List.of(), "yyyy-MM-dd'T'HH:mm:ss"));
        registry = new FilterMetadataRegistry();
        registry.initialize(Map.of(Product.class, builder.build(Product.class)));
        validator = new FilterValidator(registry);
    }

    private static FilterCondition nameEq(String value) {
        return new FilterCondition("name", Operator.EQ, value, null);
    }

    private static FilterNode nested(int depth) {
        FilterNode node = nameEq("x");
        for (int i = 1; i < depth; i++) {
            node = FilterGroup.and(node);
        }
        return node;
    }

    private void validate(FilterNode node) {
        validator.validate(new FilterRequest(node), Product.class);
    }

    @Test
    void rejectsMissingRequestOrFilter() {
        assertThatThrownBy(() -> validator.validate(null, Product.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> validate(null)).isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void rejectsNullChildNode() {
        List<FilterNode> children = new ArrayList<>();
        children.add(null);
        assertThatThrownBy(() -> new FilterGroup(LogicalOperator.AND, children))
                .isInstanceOf(NullPointerException.class);
    }

    /** Regression: a missing field used to fail with a NullPointerException (HTTP 500). */
    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void rejectsBlankField(String field) {
        assertThatThrownBy(() -> validate(new FilterCondition(field, Operator.EQ, "x", null)))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("missing a field");
    }

    @Test
    void rejectsNullField() {
        assertThatThrownBy(() -> validate(new FilterCondition(null, Operator.EQ, "x", null)))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("missing a field");
    }

    @Test
    void rejectsMissingOperator() {
        assertThatThrownBy(() -> validate(new FilterCondition("name", null, "x", null)))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("missing an operator");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name; DROP TABLE product; --",
            "name' OR '1'='1",
            "__proto__",
            "constructor.prototype",
            "internalNote",
            "category.name",
            "NAME",
            "name "
    })
    void rejectsFieldNamesOutsideTheWhitelist(String field) {
        assertThatThrownBy(() -> validate(new FilterCondition(field, Operator.EQ, "x", null)))
                .isInstanceOf(UnknownFieldException.class);
    }

    @Test
    void acceptsMaximumDepth() {
        assertThatCode(() -> validate(nested(FilterValidator.DEFAULT_MAX_DEPTH))).doesNotThrowAnyException();
    }

    /** Regression: ~450 nested groups used to overflow the stack in the translator. */
    @Test
    void rejectsExcessiveDepth() {
        assertThatThrownBy(() -> validate(nested(FilterValidator.DEFAULT_MAX_DEPTH + 1)))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("nesting depth");
        assertThatThrownBy(() -> validate(nested(5_000)))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void depthLimitAlsoAppliesToEmptyGroups() {
        FilterNode node = FilterGroup.and();
        for (int i = 0; i < FilterValidator.DEFAULT_MAX_DEPTH; i++) {
            node = FilterGroup.or(node);
        }
        FilterNode tooDeep = node;
        assertThatThrownBy(() -> validate(tooDeep)).isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: ~70k conditions exceeded PostgreSQL's 65 535 bind-parameter limit (HTTP 500). */
    @Test
    void limitsTotalConditionsAcrossTheTree() {
        List<FilterNode> atLimit = IntStream.range(0, FilterValidator.DEFAULT_MAX_CONDITIONS)
                .<FilterNode>mapToObj(i -> nameEq("n" + i)).toList();
        assertThatCode(() -> validate(FilterGroup.or(atLimit))).doesNotThrowAnyException();

        List<FilterNode> half = atLimit.subList(0, FilterValidator.DEFAULT_MAX_CONDITIONS / 2 + 1);
        assertThatThrownBy(() -> validate(FilterGroup.and(FilterGroup.or(half), FilterGroup.or(half))))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("maximum of 1000 conditions");
    }

    @Test
    void limitsValuesPerCondition() {
        List<Object> atLimit = IntStream.range(0, FilterValidator.DEFAULT_MAX_VALUES)
                .<Object>mapToObj(i -> "v" + i).toList();
        assertThatCode(() -> validate(new FilterCondition("name", Operator.IN, null, atLimit)))
                .doesNotThrowAnyException();

        List<Object> overLimit = new ArrayList<>(atLimit);
        overLimit.add("one-more");
        assertThatThrownBy(() -> validate(new FilterCondition("name", Operator.IN, null, overLimit)))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("maximum of 1000 values");
    }

    @Test
    void limitsAreConfigurable() {
        FilterValidator strict = new FilterValidator(registry, 2, 2, 2);
        assertThatThrownBy(() -> strict.validate(new FilterRequest(nested(3)), Product.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> strict.validate(new FilterRequest(
                FilterGroup.or(nameEq("a"), nameEq("b"), nameEq("c"))), Product.class))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> strict.validate(new FilterRequest(
                new FilterCondition("name", Operator.IN, null, List.of("a", "b", "c"))), Product.class))
                .isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: "value" was silently ignored when "values" was also supplied. */
    @Test
    void rejectsValueAndValuesTogether() {
        assertThatThrownBy(() -> validate(new FilterCondition("name", Operator.EQ, "a", List.of("b"))))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("either 'value' or 'values'");
    }

    @Test
    void rejectsNullOperandsInLists() {
        assertThatThrownBy(() -> validate(new FilterCondition("name", Operator.IN, null,
                Arrays.asList("a", null))))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("IS_NULL");
        assertThatThrownBy(() -> validate(new FilterCondition("price", Operator.BETWEEN, null,
                Arrays.asList(null, 10))))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void singleNullValueIsReportedAsMissingValue() {
        assertThatThrownBy(() -> validate(new FilterCondition("name", Operator.EQ, null, null)))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("exactly 1 value");
    }

    @Test
    void emptyValuesListIsRejectedForIn() {
        assertThatThrownBy(() -> validate(new FilterCondition("name", Operator.IN, null, Collections.emptyList())))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("at least 1 value");
    }

    /** Regression: NUL characters reached PostgreSQL and failed with an encoding error (HTTP 500). */
    @Test
    void rejectsNulCharacters() {
        assertThatThrownBy(() -> validate(nameEq("a\u0000b")))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("NUL");
    }

    @Test
    void acceptsEmptyGroupsAndContradictions() {
        assertThatCode(() -> validate(FilterGroup.and())).doesNotThrowAnyException();
        assertThatCode(() -> validate(FilterGroup.or())).doesNotThrowAnyException();
        assertThatCode(() -> validate(FilterGroup.and(nameEq("a"), nameEq("b"), nameEq("a"))))
                .doesNotThrowAnyException();
    }

    @Test
    void jsonArrayContainsAcceptsOptionalPath() {
        assertThat(Operator.JSON_ARRAY_CONTAINS.arity()).isEqualTo(Operator.Arity.SINGLE_OR_PAIR);
    }
}
