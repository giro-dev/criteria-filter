package dev.agiro.criteriafilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import dev.agiro.criteriafilter.validation.FilterValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Time budgets for the largest accepted filters and for rejecting oversized ones, so that the
 * request limits enforced by {@link FilterValidator} keep the endpoint responsive.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FilterPerformanceBudgetTest {

    private static final Duration ACCEPTED_BUDGET = Duration.ofSeconds(3);
    private static final Duration REJECTED_BUDGET = Duration.ofSeconds(2);

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJpaRepository products;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void seedAndWarmUp() throws Exception {
        products.deleteAll();
        products.saveAll(IntStream.range(0, 200)
                .mapToObj(i -> new Product((long) i, "product-" + i, BigDecimal.valueOf(i),
                        i % 2 == 0 ? Category.BOOK : Category.TOY, Instant.parse("2024-01-01T00:00:00Z"), true))
                .toList());
        search(orOfEquals(10), status().isOk());
    }

    @Test
    void maximumConditionCountFitsBudget() {
        String body = orOfEquals(FilterValidator.DEFAULT_MAX_CONDITIONS);
        assertTimeout(ACCEPTED_BUDGET, () -> search(body, status().isOk(), jsonPath("$.totalHits").value(200)));
    }

    @Test
    void maximumInListFitsBudget() {
        String body = inList(FilterValidator.DEFAULT_MAX_VALUES);
        assertTimeout(ACCEPTED_BUDGET, () -> search(body, status().isOk(), jsonPath("$.totalHits").value(200)));
    }

    @Test
    void maximumNestingFitsBudget() {
        String body = nested(FilterValidator.DEFAULT_MAX_DEPTH - 1);
        assertTimeout(ACCEPTED_BUDGET, () -> search(body, status().isOk(), jsonPath("$.totalHits").value(1)));
    }

    @Test
    void oversizedConditionCountIsRejectedWithinBudget() {
        String body = orOfEquals(100_000);
        assertTimeout(REJECTED_BUDGET, () -> search(body, status().isBadRequest()));
    }

    @Test
    void oversizedInListIsRejectedWithinBudget() {
        String body = inList(100_000);
        assertTimeout(REJECTED_BUDGET, () -> search(body, status().isBadRequest()));
    }

    @Test
    void excessiveNestingIsRejectedWithinBudget() {
        String body = nested(10_000);
        assertTimeout(REJECTED_BUDGET, () -> search(body, status().isBadRequest()));
    }

    private void search(String body, ResultMatcher... matchers) throws Exception {
        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpectAll(matchers);
    }

    private String orOfEquals(int count) {
        List<Map<String, Object>> conditions = IntStream.range(0, count)
                .mapToObj(i -> Map.<String, Object>of("field", "name", "operator", "EQ", "value", "product-" + i))
                .toList();
        return write(Map.of("filter", Map.of("or", conditions)));
    }

    private String inList(int count) {
        List<String> values = IntStream.range(0, count).mapToObj(i -> "product-" + i).toList();
        return write(Map.of("filter", Map.of("field", "name", "operator", "IN", "values", values)));
    }

    private static String nested(int depth) {
        return "{\"filter\":" + "{\"and\":[".repeat(depth)
                + "{\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"product-1\"}" + "]}".repeat(depth) + "}";
    }

    private String write(Object value) {
        try {
            return mapper.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
