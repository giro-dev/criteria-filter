package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.sample.Measurement;
import dev.agiro.criteriafilter.sample.MeasurementJpaRepository;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * End-to-end coverage of every standard operator, logical combination and
 * supported data type against H2.
 */
@SpringBootTest
class JpaOperatorCoverageIntegrationTest {

    private static final UUID REF_A = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID REF_B = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @Autowired
    private CriteriaRepositoryRegistry registry;

    @Autowired
    private ProductJpaRepository products;

    @Autowired
    private MeasurementJpaRepository measurements;

    @BeforeEach
    void seed() {
        measurements.deleteAll();
        products.deleteAll();
        Product cleanCode = new Product(1L, "Clean Code", new BigDecimal("35.00"), Category.BOOK,
                Instant.parse("2024-01-10T00:00:00Z"), true);
        Product javaBook = new Product(2L, "Effective Java", new BigDecimal("45.00"), Category.BOOK,
                Instant.parse("2024-03-15T00:00:00Z"), true);
        products.saveAll(List.of(
                cleanCode,
                javaBook,
                new Product(3L, "Toy Car", new BigDecimal("15.00"), Category.TOY,
                        Instant.parse("2024-05-01T00:00:00Z"), false),
                new Product(4L, "Chocolate", new BigDecimal("5.50"), Category.FOOD, null, true),
                new Product(5L, "100% Cotton_Shirt", null, Category.TOY,
                        Instant.parse("2024-06-01T00:00:00Z"), true)
        ));
        measurements.saveAll(List.of(
                new Measurement(1L, "Ünïcödé café ☕", 1.5, 10, true, LocalDate.of(2024, 1, 1),
                        OffsetDateTime.parse("2024-01-01T10:00:00+02:00"), REF_A, cleanCode),
                new Measurement(2L, "100% pure_water", 2.5, Integer.MAX_VALUE, false, LocalDate.of(2024, 2, 29),
                        OffsetDateTime.parse("2024-01-01T09:00:00Z"), REF_B, javaBook),
                new Measurement(3L, null, 1e300, Integer.MIN_VALUE, null, LocalDate.of(2023, 12, 31),
                        OffsetDateTime.parse("2024-01-01T07:00:00-05:00"), null, null),
                new Measurement(4L, "100 percent pure water", 0.0, 0, true, LocalDate.of(2024, 3, 1),
                        null, UUID.randomUUID(), cleanCode)
        ));
    }

    @AfterEach
    void cleanUp() {
        measurements.deleteAll();
    }

    private List<String> productNames(FilterNode filter) {
        return registry.resolve(Product.class).filter(new FilterRequest(filter), new PageRequest(0, 100))
                .content().stream().map(Product::getName).toList();
    }

    private List<Long> measurementIds(FilterNode filter) {
        return registry.resolve(Measurement.class).filter(new FilterRequest(filter), new PageRequest(0, 100))
                .content().stream().map(Measurement::getId).toList();
    }

    private static FilterCondition cond(String field, Operator operator, Object value) {
        return new FilterCondition(field, operator, value, null);
    }

    private static FilterCondition multi(String field, Operator operator, Object... values) {
        return new FilterCondition(field, operator, null, List.of(values));
    }

    // ---- every standard operator ----------------------------------------------------------

    @Test
    void eqAndNe() {
        assertThat(productNames(cond("name", Operator.EQ, "Toy Car"))).containsExactly("Toy Car");
        assertThat(productNames(cond("category", Operator.NE, "BOOK")))
                .containsExactlyInAnyOrder("Toy Car", "Chocolate", "100% Cotton_Shirt");
    }

    @Test
    void neExcludesNullsLikeSql() {
        assertThat(productNames(cond("price", Operator.NE, "35.00")))
                .containsExactlyInAnyOrder("Effective Java", "Toy Car", "Chocolate");
    }

    @Test
    void comparisonOperators() {
        assertThat(productNames(cond("price", Operator.GT, 35))).containsExactly("Effective Java");
        assertThat(productNames(cond("price", Operator.GTE, "35.00")))
                .containsExactlyInAnyOrder("Clean Code", "Effective Java");
        assertThat(productNames(cond("price", Operator.LT, 15))).containsExactly("Chocolate");
        assertThat(productNames(cond("price", Operator.LTE, 15.0)))
                .containsExactlyInAnyOrder("Toy Car", "Chocolate");
    }

    @Test
    void likeIsCaseInsensitiveContains() {
        assertThat(productNames(cond("name", Operator.LIKE, "CO"))).containsExactlyInAnyOrder(
                "Clean Code", "Chocolate", "100% Cotton_Shirt");
        assertThat(productNames(cond("name", Operator.LIKE, ""))).hasSize(5);
    }

    /** Regression: '%' and '_' in LIKE values used to act as SQL wildcards. */
    @Test
    void likeTreatsWildcardsLiterally() {
        assertThat(productNames(cond("name", Operator.LIKE, "%"))).containsExactly("100% Cotton_Shirt");
        assertThat(productNames(cond("name", Operator.LIKE, "_"))).containsExactly("100% Cotton_Shirt");
        assertThat(productNames(cond("name", Operator.LIKE, "C_ean"))).isEmpty();
        assertThat(productNames(cond("name", Operator.LIKE, "\\"))).isEmpty();
        assertThat(measurementIds(cond("label", Operator.LIKE, "pure_water"))).containsExactly(2L);
        assertThat(measurementIds(cond("label", Operator.LIKE, "100%"))).containsExactly(2L);
    }

    @Test
    void inAndBetween() {
        assertThat(productNames(multi("category", Operator.IN, "TOY", "FOOD")))
                .containsExactlyInAnyOrder("Toy Car", "Chocolate", "100% Cotton_Shirt");
        assertThat(productNames(multi("id", Operator.IN, 1, "2", 999L)))
                .containsExactlyInAnyOrder("Clean Code", "Effective Java");
        assertThat(productNames(multi("price", Operator.BETWEEN, "15.00", 35)))
                .containsExactlyInAnyOrder("Clean Code", "Toy Car");
        assertThat(productNames(multi("price", Operator.BETWEEN, 40, 10))).isEmpty();
    }

    @Test
    void nullChecks() {
        assertThat(productNames(cond("createdAt", Operator.IS_NULL, null))).containsExactly("Chocolate");
        assertThat(productNames(cond("price", Operator.IS_NOT_NULL, null))).hasSize(4);
    }

    // ---- logical combinations ---------------------------------------------------------------

    @Test
    void nestedAndOr() {
        FilterNode filter = FilterGroup.and(
                cond("active", Operator.EQ, true),
                FilterGroup.or(
                        cond("category", Operator.EQ, "FOOD"),
                        FilterGroup.and(cond("category", Operator.EQ, "BOOK"), cond("price", Operator.GT, 40))));
        assertThat(productNames(filter)).containsExactlyInAnyOrder("Chocolate", "Effective Java");
    }

    /** Regression: an empty OR group used to match every row. */
    @Test
    void emptyGroupsFollowBooleanIdentities() {
        assertThat(productNames(FilterGroup.and())).hasSize(5);
        assertThat(productNames(FilterGroup.or())).isEmpty();
        assertThat(productNames(FilterGroup.or(FilterGroup.and()))).hasSize(5);
        assertThat(productNames(FilterGroup.and(FilterGroup.or(), cond("name", Operator.EQ, "Toy Car")))).isEmpty();
        assertThat(productNames(FilterGroup.or(FilterGroup.or(), cond("name", Operator.EQ, "Toy Car"))))
                .containsExactly("Toy Car");
    }

    @Test
    void contradictionsAndDuplicates() {
        assertThat(productNames(FilterGroup.and(cond("price", Operator.GT, 20), cond("price", Operator.LT, 10))))
                .isEmpty();
        assertThat(productNames(FilterGroup.and(cond("name", Operator.EQ, "Toy Car"),
                cond("name", Operator.EQ, "Toy Car")))).containsExactly("Toy Car");
        assertThat(productNames(FilterGroup.or(cond("name", Operator.EQ, "Toy Car"),
                cond("name", Operator.EQ, "Toy Car")))).containsExactly("Toy Car");
    }

    @Test
    void injectionAttemptsInValuesAreBoundAsLiterals() {
        assertThat(productNames(cond("name", Operator.EQ, "x' OR '1'='1"))).isEmpty();
        assertThat(productNames(cond("name", Operator.LIKE, "') OR 1=1 --"))).isEmpty();
        assertThat(productNames(multi("name", Operator.IN, "a'); DROP TABLE product; --"))).isEmpty();
        assertThat(productNames(FilterGroup.and())).hasSize(5);
    }

    // ---- pagination -------------------------------------------------------------------------

    @Test
    void paginationReportsHasMore() {
        var repo = registry.resolve(Product.class);
        FilterRequest all = new FilterRequest(FilterGroup.and());
        FilterResult<Product> first = repo.filter(all, new PageRequest(0, 2));
        FilterResult<Product> last = repo.filter(all, new PageRequest(2, 2));
        FilterResult<Product> beyond = repo.filter(all, new PageRequest(10, 2));
        assertThat(first.content()).hasSize(2);
        assertThat(first.hasMore()).isTrue();
        assertThat(first.totalHits()).isEqualTo(5);
        assertThat(last.content()).hasSize(1);
        assertThat(last.hasMore()).isFalse();
        assertThat(beyond.content()).isEmpty();
        assertThat(beyond.hasMore()).isFalse();
        assertThat(repo.filter(all, new PageRequest(0, 5)).hasMore()).isFalse();
    }

    // ---- data types -------------------------------------------------------------------------

    @Test
    void unicodeStrings() {
        assertThat(measurementIds(cond("label", Operator.EQ, "Ünïcödé café ☕"))).containsExactly(1L);
        assertThat(measurementIds(cond("label", Operator.LIKE, "CAFÉ ☕"))).containsExactly(1L);
        assertThat(measurementIds(cond("label", Operator.IS_NULL, null))).containsExactly(3L);
    }

    @Test
    void integerBoundaries() {
        assertThat(measurementIds(cond("count", Operator.EQ, "2147483647"))).containsExactly(2L);
        assertThat(measurementIds(cond("count", Operator.LTE, Integer.MIN_VALUE))).containsExactly(3L);
        assertThat(measurementIds(multi("count", Operator.BETWEEN, Integer.MIN_VALUE, Integer.MAX_VALUE)))
                .hasSize(4);
        assertThatThrownBy(() -> measurementIds(cond("count", Operator.GT, "2147483648")))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> measurementIds(cond("count", Operator.EQ, 1.5)))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void floatingPointValues() {
        assertThat(measurementIds(cond("reading", Operator.GT, 1e299))).containsExactly(3L);
        assertThat(measurementIds(cond("reading", Operator.EQ, "0"))).containsExactly(4L);
        assertThat(measurementIds(cond("reading", Operator.EQ, "-0.0"))).containsExactly(4L);
    }

    /** Regression: NaN / Infinity were passed through to the database. */
    @Test
    void nonFiniteNumbersAreRejected() {
        assertThatThrownBy(() -> measurementIds(cond("reading", Operator.LT, "Infinity")))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> measurementIds(cond("reading", Operator.EQ, "NaN")))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> productNames(cond("price", Operator.GT, "NaN")))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void booleans() {
        assertThat(measurementIds(cond("flagged", Operator.EQ, "TRUE"))).containsExactlyInAnyOrder(1L, 4L);
        assertThat(measurementIds(cond("flagged", Operator.EQ, false))).containsExactly(2L);
        assertThat(measurementIds(cond("flagged", Operator.NE, false))).containsExactlyInAnyOrder(1L, 4L);
        assertThat(measurementIds(cond("flagged", Operator.IS_NULL, null))).containsExactly(3L);
    }

    /** Regression: "yes" / 1 used to be coerced to {@code false} and match the wrong rows. */
    @Test
    void ambiguousBooleansAreRejected() {
        assertThatThrownBy(() -> measurementIds(cond("flagged", Operator.EQ, "yes")))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> productNames(cond("active", Operator.EQ, 1)))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void localDates() {
        assertThat(measurementIds(multi("day", Operator.BETWEEN, "2024-01-01", "2024-02-29")))
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(measurementIds(cond("day", Operator.EQ, "2024-02-29"))).containsExactly(2L);
        assertThat(measurementIds(cond("day", Operator.LT, "2024-01-01"))).containsExactly(3L);
        assertThatThrownBy(() -> measurementIds(cond("day", Operator.EQ, "2023-02-29")))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void offsetDateTimesCompareByInstantAcrossTimezones() {
        // Stored: #1 08:00Z (+02:00), #2 09:00Z, #3 12:00Z (-05:00), #4 null
        assertThat(measurementIds(cond("takenAt", Operator.GT, "2024-01-01T08:30:00Z")))
                .containsExactlyInAnyOrder(2L, 3L);
        assertThat(measurementIds(cond("takenAt", Operator.LTE, "2024-01-01T04:00:00-05:00")))
                .containsExactlyInAnyOrder(1L, 2L);
        assertThat(measurementIds(cond("takenAt", Operator.IS_NULL, null))).containsExactly(4L);
        assertThatThrownBy(() -> measurementIds(cond("takenAt", Operator.GT, "2024-01-01T08:30:00")))
                .isInstanceOf(FilterTranslationException.class);
    }

    @Test
    void instantsAcceptOffsets() {
        assertThat(productNames(cond("createdAt", Operator.LT, "2024-01-10T02:00:00+02:00"))).isEmpty();
        assertThat(productNames(cond("createdAt", Operator.LTE, "2024-01-10T02:00:00+02:00")))
                .containsExactly("Clean Code");
    }

    @Test
    void uuids() {
        assertThat(measurementIds(cond("ref", Operator.EQ, REF_A.toString()))).containsExactly(1L);
        assertThat(measurementIds(cond("ref", Operator.NE, REF_A.toString()))).containsExactlyInAnyOrder(2L, 4L);
        assertThatThrownBy(() -> measurementIds(cond("ref", Operator.EQ, "not-a-uuid")))
                .isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: EQ on an association used to fail with a Hibernate SemanticException (HTTP 500). */
    @Test
    void associationsCompareByIdentifier() {
        assertThat(measurementIds(cond("product", Operator.EQ, 1))).containsExactlyInAnyOrder(1L, 4L);
        assertThat(measurementIds(cond("product", Operator.EQ, "2"))).containsExactly(2L);
        assertThat(measurementIds(cond("product", Operator.NE, 1))).containsExactly(2L);
        assertThat(measurementIds(cond("product", Operator.IS_NULL, null))).containsExactly(3L);
        assertThatThrownBy(() -> measurementIds(cond("product", Operator.EQ, "abc")))
                .isInstanceOf(FilterTranslationException.class);
    }

    /** Regression: arrays/objects passed to scalar fields were stringified and silently compared. */
    @Test
    void structuredValuesForScalarFieldsAreRejected() {
        assertThatThrownBy(() -> productNames(cond("name", Operator.EQ, List.of("Toy Car"))))
                .isInstanceOf(FilterTranslationException.class);
        assertThatThrownBy(() -> productNames(cond("price", Operator.GT, Map.of("a", 1))))
                .isInstanceOf(FilterTranslationException.class);
    }
}
