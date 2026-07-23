package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.LogicalOperator;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class JpaCriteriaRepositoryIntegrationTest {

    @Autowired
    private CriteriaRepositoryRegistry registry;

    @Autowired
    private ProductJpaRepository products;

    @BeforeEach
    void seed() {
        products.deleteAll();
        products.saveAll(List.of(
                new Product(1L, "Clean Code", new BigDecimal("35.00"), Category.BOOK,
                        Instant.parse("2024-01-10T00:00:00Z"), true),
                new Product(2L, "Effective Java", new BigDecimal("45.00"), Category.BOOK,
                        Instant.parse("2024-03-15T00:00:00Z"), true),
                new Product(3L, "Toy Car", new BigDecimal("15.00"), Category.TOY,
                        Instant.parse("2024-05-01T00:00:00Z"), false),
                new Product(4L, "Chocolate", new BigDecimal("5.50"), Category.FOOD,
                        Instant.parse("2024-06-20T00:00:00Z"), true)
        ));
    }

    private CriteriaRepository<Product, ?> repo() {
        return registry.resolve(Product.class);
    }

    @Test
    void filtersByLikeAndEnum() {
        FilterRequest request = new FilterRequest(new FilterGroup(LogicalOperator.AND, List.of(
                new FilterCondition("name", Operator.LIKE, "java", null),
                new FilterCondition("category", Operator.EQ, "BOOK", null)
        )));

        FilterResult<Product> result = repo().filter(request, new PageRequest(0, 10));

        assertThat(result.content()).extracting(Product::getName).containsExactly("Effective Java");
        assertThat(result.totalHits()).isEqualTo(1);
        assertThat(result.hasMore()).isFalse();
    }

    @Test
    void filtersByNumericBetweenWithStringOperands() {
        FilterRequest request = new FilterRequest(
                new FilterCondition("price", Operator.BETWEEN, null, List.of("10.00", "40.00")));

        FilterResult<Product> result = repo().filter(request, new PageRequest(0, 10));

        assertThat(result.content()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Clean Code", "Toy Car");
    }

    @Test
    void filtersByInstantComparison() {
        FilterRequest request = new FilterRequest(
                new FilterCondition("createdAt", Operator.GT, "2024-04-01T00:00:00Z", null));

        FilterResult<Product> result = repo().filter(request, new PageRequest(0, 10));

        assertThat(result.content()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Toy Car", "Chocolate");
    }

    @Test
    void combinesWithOrAndPaginatesWithHasMore() {
        FilterRequest request = new FilterRequest(new FilterGroup(LogicalOperator.OR, List.of(
                new FilterCondition("category", Operator.EQ, "BOOK", null),
                new FilterCondition("active", Operator.EQ, true, null)
        )));

        FilterResult<Product> firstPage = repo().filter(request, new PageRequest(0, 2));

        // Clean Code, Effective Java, Chocolate match -> 3 total.
        assertThat(firstPage.totalHits()).isEqualTo(3);
        assertThat(firstPage.content()).hasSize(2);
        assertThat(firstPage.hasMore()).isTrue();
    }

    @Test
    void filtersByInList() {
        FilterRequest request = new FilterRequest(
                new FilterCondition("category", Operator.IN, null, List.of("TOY", "FOOD")));

        FilterResult<Product> result = repo().filter(request, new PageRequest(0, 10));

        assertThat(result.content()).extracting(Product::getName)
                .containsExactlyInAnyOrder("Toy Car", "Chocolate");
    }
}
