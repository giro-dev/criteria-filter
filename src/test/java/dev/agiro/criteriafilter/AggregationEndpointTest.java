package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.annotation.EnableFilterEndpoint;
import dev.agiro.criteriafilter.interceptor.FilterContext;
import dev.agiro.criteriafilter.interceptor.FilterInterceptor;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.sample.MeasurementJpaRepository;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * {@code POST /search/aggregate} on inherited and annotation-registered endpoints.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AggregationEndpointTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJpaRepository products;

    @Autowired
    private MeasurementJpaRepository measurements;

    @BeforeEach
    void seed() {
        measurements.deleteAll();
        products.deleteAll();
        products.saveAll(List.of(
                new Product(1L, "Clean Code", new BigDecimal("35.00"), Category.BOOK,
                        Instant.parse("2024-01-10T00:00:00Z"), true),
                new Product(2L, "Effective Java", new BigDecimal("45.00"), Category.BOOK,
                        Instant.parse("2024-03-15T00:00:00Z"), true),
                new Product(3L, "Toy Car", new BigDecimal("15.00"), Category.TOY,
                        Instant.parse("2024-05-01T00:00:00Z"), false),
                new Product(4L, "Chocolate", new BigDecimal("5.50"), Category.FOOD, null, true)
        ));
    }

    private ResultActions aggregate(String path, String body) throws Exception {
        return mockMvc.perform(post(path).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    @Test
    void groupsWithFilterAndReturnsRows() throws Exception {
        aggregate("/products/search/aggregate", """
                {
                  "filter": {"and": [{"field": "price", "operator": "GT", "value": 10}]},
                  "groupBy": ["category"],
                  "aggregations": [
                    {"field": "price", "function": "SUM", "alias": "totalPrice"},
                    {"function": "COUNT", "alias": "n"}
                  ]
                }
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(2)))
                .andExpect(jsonPath("$.rows[0].category").value("BOOK"))
                .andExpect(jsonPath("$.rows[0].totalPrice").value(80.0))
                .andExpect(jsonPath("$.rows[0].n").value(2))
                .andExpect(jsonPath("$.rows[1].category").value("TOY"))
                .andExpect(jsonPath("$.rows[1].n").value(1));
    }

    @Test
    void filterIsOptionalAndDefaultAliasesAreDerived() throws Exception {
        aggregate("/products/search/aggregate", """
                {"aggregations": [{"function": "COUNT"}, {"field": "price", "function": "MAX"},
                                  {"field": "category", "function": "COUNT_DISTINCT"}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].count").value(4))
                .andExpect(jsonPath("$.rows[0].max_price").value(45.0))
                .andExpect(jsonPath("$.rows[0].count_distinct_category").value(3));
    }

    @Test
    void unknownGroupByFieldIs400UnknownField() throws Exception {
        aggregate("/products/search/aggregate", """
                {"groupBy": ["internalNote"], "aggregations": [{"function": "COUNT"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.field").value("internalNote"));
    }

    @Test
    void unknownAggregationFieldIs400UnknownField() throws Exception {
        aggregate("/products/search/aggregate", """
                {"aggregations": [{"field": "nope", "function": "MIN"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.field").value("nope"));
    }

    @Test
    void invalidFilterIsRejectedLikeSearch() throws Exception {
        aggregate("/products/search/aggregate", """
                {"filter": {"field": "active", "operator": "GT", "value": true},
                 "aggregations": [{"function": "COUNT"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_OPERATOR"));
    }

    @Test
    void incompatibleFunctionIs400UnsupportedAggregation() throws Exception {
        aggregate("/products/search/aggregate", """
                {"aggregations": [{"field": "name", "function": "AVG", "alias": "x"}]}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_AGGREGATION"))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    void missingAggregationsIs400() throws Exception {
        aggregate("/products/search/aggregate", """
                {"groupBy": ["category"]}
                """)
                .andExpect(status().isBadRequest());
        aggregate("/products/search/aggregate", """
                {"groupBy": ["category"], "aggregations": []}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FILTER"));
    }

    @Test
    void unknownFunctionIs400() throws Exception {
        aggregate("/products/search/aggregate", """
                {"aggregations": [{"field": "price", "function": "MEDIAN"}]}
                """)
                .andExpect(status().isBadRequest());
    }

    @Test
    void annotationEndpointRunsOptInInterceptors() throws Exception {
        aggregate("/books-only/search/aggregate", """
                {"groupBy": ["category"], "aggregations": [{"function": "COUNT", "alias": "n"}]}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows", hasSize(1)))
                .andExpect(jsonPath("$.rows[0].category").value("BOOK"))
                .andExpect(jsonPath("$.rows[0].n").value(2));
    }

    @Test
    void aggregateEndpointCanBeDisabledOnAnnotation() throws Exception {
        aggregate("/no-aggregate/search/aggregate", """
                {"aggregations": [{"function": "COUNT"}]}
                """)
                .andExpect(status().is4xxClientError());
    }

    @TestConfiguration
    static class Config {
        @Bean
        BooksOnlyInterceptor booksOnlyInterceptor() {
            return new BooksOnlyInterceptor();
        }

        @Bean
        BooksOnlyController booksOnlyController() {
            return new BooksOnlyController();
        }

        @Bean
        NoAggregateController noAggregateController() {
            return new NoAggregateController();
        }
    }

    static class BooksOnlyInterceptor implements FilterInterceptor<Product> {
        @Override
        public Class<Product> entityType() {
            return Product.class;
        }

        @Override
        public boolean global() {
            return false;
        }

        @Override
        public FilterResult<Product> preFilter(FilterContext<Product> context) {
            context.addFilter("category", Operator.EQ, "BOOK");
            return null;
        }
    }

    @RestController
    @RequestMapping("/books-only")
    @EnableFilterEndpoint(entity = Product.class, interceptors = BooksOnlyInterceptor.class)
    static class BooksOnlyController {
    }

    @RestController
    @RequestMapping("/no-aggregate")
    @EnableFilterEndpoint(entity = Product.class, includeAggregate = false)
    static class NoAggregateController {
    }
}
