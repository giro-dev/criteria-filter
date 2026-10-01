package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.model.AggregateFunction;
import dev.agiro.criteriafilter.model.AggregationRequest;
import dev.agiro.criteriafilter.model.AggregationSpec;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.repository.AggregationResult;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.jpa.JpaCriteriaRepository;
import dev.agiro.criteriafilter.repository.jpa.JpaSpecificationTranslator;
import dev.agiro.criteriafilter.sample.Measurement;
import dev.agiro.criteriafilter.sample.MeasurementJpaRepository;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import jakarta.persistence.EntityManager;
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

import static dev.agiro.criteriafilter.model.AggregateFunction.AVG;
import static dev.agiro.criteriafilter.model.AggregateFunction.COUNT;
import static dev.agiro.criteriafilter.model.AggregateFunction.COUNT_DISTINCT;
import static dev.agiro.criteriafilter.model.AggregateFunction.MAX;
import static dev.agiro.criteriafilter.model.AggregateFunction.MIN;
import static dev.agiro.criteriafilter.model.AggregateFunction.SUM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Aggregation queries ({@code CriteriaRepository#aggregate}) against H2.
 */
@SpringBootTest
class JpaAggregationIntegrationTest {

    @Autowired
    private CriteriaRepositoryRegistry registry;

    @Autowired
    private FilterMetadataRegistry metadataRegistry;

    @Autowired
    private EntityManager entityManager;

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
                new Measurement(1L, "a", 1.5, 10, true, LocalDate.of(2024, 1, 1),
                        OffsetDateTime.parse("2024-01-01T10:00:00Z"), null, cleanCode),
                new Measurement(2L, "b", 2.5, 7, false, LocalDate.of(2024, 2, 29),
                        OffsetDateTime.parse("2024-01-01T09:00:00Z"), null, javaBook),
                new Measurement(3L, "c", 4.0, 3, null, LocalDate.of(2023, 12, 31), null, null, null),
                new Measurement(4L, "d", 0.5, 0, true, LocalDate.of(2024, 3, 1), null, null, cleanCode)
        ));
    }

    @AfterEach
    void cleanUp() {
        measurements.deleteAll();
    }

    private AggregationResult products(FilterNode filter, List<String> groupBy, AggregationSpec... specs) {
        return registry.resolve(Product.class).aggregate(new AggregationRequest(filter, groupBy, List.of(specs)));
    }

    private AggregationResult measurements(FilterNode filter, List<String> groupBy, AggregationSpec... specs) {
        return registry.resolve(Measurement.class)
                .aggregate(new AggregationRequest(filter, groupBy, List.of(specs)));
    }

    private static AggregationSpec agg(String field, AggregateFunction function, String alias) {
        return new AggregationSpec(field, function, alias);
    }

    private static double num(Object value) {
        return ((Number) value).doubleValue();
    }

    @Test
    void groupsBySingleFieldOrderedByGroupKey() {
        AggregationResult result = products(null, List.of("category"),
                agg("price", SUM, "totalPrice"), agg(null, COUNT, "n"));

        assertThat(result.rows()).extracting(r -> r.get("category"))
                .containsExactly(Category.BOOK, Category.FOOD, Category.TOY);
        assertThat(result.rows()).extracting(r -> r.get("n")).containsExactly(2L, 1L, 2L);
        assertThat(num(result.rows().get(0).get("totalPrice"))).isEqualTo(80.0);
        assertThat(num(result.rows().get(1).get("totalPrice"))).isEqualTo(5.5);
        assertThat(num(result.rows().get(2).get("totalPrice"))).isEqualTo(15.0);
        assertThat(result.rows().get(0).keySet()).containsExactly("category", "totalPrice", "n");
    }

    @Test
    void groupsByMultipleFields() {
        AggregationResult result = products(null, List.of("category", "active"), agg(null, COUNT, "n"));

        assertThat(result.rows()).containsExactly(
                Map.of("category", Category.BOOK, "active", true, "n", 2L),
                Map.of("category", Category.FOOD, "active", true, "n", 1L),
                Map.of("category", Category.TOY, "active", false, "n", 1L),
                Map.of("category", Category.TOY, "active", true, "n", 1L));
    }

    @Test
    void withoutGroupByReturnsSingleTotalsRow() {
        AggregationResult result = products(null, List.of(),
                agg(null, COUNT, null), agg("price", SUM, null), agg("category", COUNT_DISTINCT, null));

        assertThat(result.rows()).hasSize(1);
        Map<String, Object> row = result.rows().get(0);
        assertThat(row.get("count")).isEqualTo(5L);
        assertThat(num(row.get("sum_price"))).isEqualTo(100.5);
        assertThat(row.get("count_distinct_category")).isEqualTo(3L);
    }

    @Test
    void sumAndAvg() {
        Map<String, Object> book = products(null, List.of("category"),
                agg("price", SUM, "sum"), agg("price", AVG, "avg")).rows().get(0);
        assertThat(book.get("sum")).isInstanceOf(BigDecimal.class);
        assertThat((BigDecimal) book.get("sum")).isEqualByComparingTo("80.00");
        assertThat(num(book.get("avg"))).isEqualTo(40.0);
    }

    @Test
    void minAndMaxOnNumbersStringsAndDates() {
        Map<String, Object> row = products(null, List.of(),
                agg("price", MIN, "minPrice"), agg("price", MAX, "maxPrice"),
                agg("name", MIN, "firstName"), agg("name", MAX, "lastName"),
                agg("createdAt", MIN, "oldest"), agg("createdAt", MAX, "newest")).rows().get(0);

        assertThat((BigDecimal) row.get("minPrice")).isEqualByComparingTo("5.50");
        assertThat((BigDecimal) row.get("maxPrice")).isEqualByComparingTo("45.00");
        assertThat(row.get("firstName")).isEqualTo("100% Cotton_Shirt");
        assertThat(row.get("lastName")).isEqualTo("Toy Car");
        assertThat(row.get("oldest")).isEqualTo(Instant.parse("2024-01-10T00:00:00Z"));
        assertThat(row.get("newest")).isEqualTo(Instant.parse("2024-06-01T00:00:00Z"));
    }

    @Test
    void countRowsVersusCountFieldVersusCountDistinct() {
        Map<String, Object> toy = products(null, List.of("category"),
                agg(null, COUNT, "rows"), agg("price", COUNT, "priced"),
                agg("active", COUNT_DISTINCT, "states")).rows().get(2);

        assertThat(toy.get("category")).isEqualTo(Category.TOY);
        assertThat(toy.get("rows")).isEqualTo(2L);
        assertThat(toy.get("priced")).isEqualTo(1L); // COUNT(field) ignores nulls
        assertThat(toy.get("states")).isEqualTo(2L);
    }

    @Test
    void sumOfEmptyOrAllNullGroupIsNull() {
        AggregationResult result = products(new FilterCondition("id", Operator.EQ, 5, null), List.of("category"),
                agg("price", SUM, "sum"), agg(null, COUNT, "n"));
        assertThat(result.rows()).hasSize(1);
        assertThat(result.rows().get(0)).containsEntry("n", 1L).containsEntry("sum", null);
    }

    @Test
    void appliesFilterTreeAsWhereClause() {
        FilterNode filter = FilterGroup.and(
                new FilterCondition("active", Operator.EQ, true, null),
                FilterGroup.or(
                        new FilterCondition("category", Operator.EQ, "BOOK", null),
                        new FilterCondition("price", Operator.LT, 10, null)));

        AggregationResult result = products(filter, List.of("category"),
                agg("price", SUM, "total"), agg(null, COUNT, "n"));

        assertThat(result.rows()).extracting(r -> r.get("category")).containsExactly(Category.BOOK, Category.FOOD);
        assertThat(result.rows()).extracting(r -> r.get("n")).containsExactly(2L, 1L);
    }

    @Test
    void filterMatchingNothingYieldsNoGroupsButOneTotalsRow() {
        FilterNode none = new FilterCondition("name", Operator.EQ, "nope", null);
        assertThat(products(none, List.of("category"), agg(null, COUNT, "n")).rows()).isEmpty();

        AggregationResult totals = products(none, List.of(), agg(null, COUNT, "n"), agg("price", MAX, "max"));
        assertThat(totals.rows()).hasSize(1);
        assertThat(totals.rows().get(0)).containsEntry("n", 0L).containsEntry("max", null);
    }

    @Test
    void groupsByAssociationIdentifierAndAggregatesPrimitiveAndBoxedNumbers() {
        AggregationResult result = measurements(new FilterCondition("product", Operator.IS_NOT_NULL, null, null),
                List.of("product"),
                agg("count", SUM, "items"), agg("reading", AVG, "avgReading"),
                agg("day", MAX, "lastDay"), agg(null, COUNT, "n"));

        assertThat(result.rows()).hasSize(2);
        Map<String, Object> first = result.rows().get(0);
        assertThat(first.get("product")).isEqualTo(1L);
        assertThat(num(first.get("items"))).isEqualTo(10.0);
        assertThat(num(first.get("avgReading"))).isEqualTo(1.0);
        assertThat(first.get("lastDay")).isEqualTo(LocalDate.of(2024, 3, 1));
        assertThat(first.get("n")).isEqualTo(2L);
        assertThat(result.rows().get(1)).containsEntry("product", 2L).containsEntry("n", 1L);
    }

    @Test
    void countDistinctOnAssociationCountsDistinctNonNullTargets() {
        Map<String, Object> row = measurements(null, List.of(), agg("product", COUNT_DISTINCT, "products"))
                .rows().get(0);
        assertThat(row.get("products")).isEqualTo(2L);
    }

    @Test
    void rejectsResultsAboveTheGroupLimit() {
        var limited = new JpaCriteriaRepository<>(entityManager, Product.class,
                metadataRegistry.require(Product.class), new JpaSpecificationTranslator(), 2);

        assertThatThrownBy(() -> limited.aggregate(new AggregationRequest(null, List.of("category"),
                List.of(agg(null, COUNT, "n")))))
                .isInstanceOf(FilterTranslationException.class)
                .hasMessageContaining("maximum of 2 groups");
        assertThat(limited.aggregate(new AggregationRequest(new FilterCondition("category", Operator.IN, null,
                List.of("BOOK", "TOY")), List.of("category"), List.of(agg(null, COUNT, "n")))).rows()).hasSize(2);
    }
}
