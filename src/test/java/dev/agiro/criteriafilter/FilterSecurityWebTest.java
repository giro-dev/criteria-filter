package dev.agiro.criteriafilter;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Hostile and malformed inputs must produce 4xx responses, never 500s or extra rows.
 */
@SpringBootTest
@AutoConfigureMockMvc
class FilterSecurityWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJpaRepository products;

    private final ObjectMapper mapper = new ObjectMapper();

    @BeforeEach
    void seed() {
        products.deleteAll();
        products.saveAll(List.of(
                new Product(1L, "Clean Code", new BigDecimal("35.00"), Category.BOOK,
                        Instant.parse("2024-01-10T00:00:00Z"), true),
                new Product(2L, "Toy Car", new BigDecimal("15.00"), Category.TOY,
                        Instant.parse("2024-05-01T00:00:00Z"), false)
        ));
    }

    private ResultActions search(String body) throws Exception {
        return search(body, "");
    }

    private ResultActions search(String body, String query) throws Exception {
        return mockMvc.perform(post("/products/search" + query)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String eq(String field, String valueJson) {
        return "{\"filter\":{\"field\":\"" + field + "\",\"operator\":\"EQ\",\"value\":" + valueJson + "}}";
    }

    private static String nested(int depth) {
        return "{\"filter\":" + "{\"and\":[".repeat(depth)
                + "{\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"x\"}" + "]}".repeat(depth) + "}";
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "name; DROP TABLE product; --",
            "name) OR (1=1",
            "__proto__",
            "internalNote",
            "class.classLoader"
    })
    void maliciousFieldNamesAreRejected(String field) throws Exception {
        search(mapper.writeValueAsString(Map.of("filter", Map.of("field", field, "operator", "EQ", "value", "x"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_FIELD"));
        search("{\"filter\":{\"and\":[]}}").andExpect(jsonPath("$.totalHits").value(2));
    }

    @Test
    void injectionInValuesMatchesNothing() throws Exception {
        search(eq("name", "\"x' OR '1'='1\"")).andExpect(status().isOk()).andExpect(jsonPath("$.totalHits").value(0));
        search("{\"filter\":{\"field\":\"name\",\"operator\":\"LIKE\",\"value\":\"%' OR 1=1 --\"}}")
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalHits").value(0));
    }

    @ParameterizedTest
    @ValueSource(strings = {"eq", "EQ OR 1=1", "DROP", "LIKE; --", ""})
    void smuggledOperatorsAreRejected(String operator) throws Exception {
        search("{\"filter\":{\"field\":\"name\",\"operator\":\"" + operator + "\",\"value\":\"x\"}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void operatorNotAllowedForFieldIsRejected() throws Exception {
        search("{\"filter\":{\"field\":\"active\",\"operator\":\"GT\",\"value\":true}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNSUPPORTED_OPERATOR"));
    }

    /** Regression: a missing field used to produce a NullPointerException (HTTP 500). */
    @Test
    void missingFieldIsRejected() throws Exception {
        search("{\"filter\":{\"operator\":\"EQ\",\"value\":\"x\"}}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_FILTER"));
    }

    @Test
    void missingOrEmptyFilterIsRejected() throws Exception {
        search("{}").andExpect(status().isBadRequest());
        search("{\"filter\":null}").andExpect(status().isBadRequest());
        search("{\"filter\":{\"and\":[null]}}").andExpect(status().isBadRequest());
    }

    @Test
    void emptyGroupsAreAccepted() throws Exception {
        search("{\"filter\":{\"and\":[]}}").andExpect(status().isOk()).andExpect(jsonPath("$.totalHits").value(2));
        search("{\"filter\":{\"or\":[]}}").andExpect(status().isOk()).andExpect(jsonPath("$.totalHits").value(0));
    }

    @Test
    void malformedJsonIsRejected() throws Exception {
        search("{\"filter\":{\"field\":\"name\"").andExpect(status().isBadRequest());
        search("not json").andExpect(status().isBadRequest());
    }

    /** Regression: ~450 nested groups used to cause a StackOverflowError (HTTP 500). */
    @Test
    void deeplyNestedPayloadsAreRejected() throws Exception {
        search(nested(31)).andExpect(status().isOk());
        search(nested(100)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Filter exceeds the maximum nesting depth of 32"));
        search(nested(5_000)).andExpect(status().isBadRequest());
    }

    @Test
    void oversizedPayloadsAreRejected() throws Exception {
        List<Map<String, Object>> conditions = IntStream.range(0, 1_001)
                .mapToObj(i -> Map.<String, Object>of("field", "name", "operator", "EQ", "value", "n" + i))
                .toList();
        search(mapper.writeValueAsString(Map.of("filter", Map.of("or", conditions))))
                .andExpect(status().isBadRequest());

        List<String> values = IntStream.range(0, 1_001).mapToObj(i -> "v" + i).toList();
        search(mapper.writeValueAsString(Map.of("filter",
                Map.of("field", "name", "operator", "IN", "values", values))))
                .andExpect(status().isBadRequest());
    }

    /** Regression: invalid paging parameters used to return HTTP 500. */
    @ParameterizedTest
    @ValueSource(strings = {"?page=-1", "?size=0", "?size=-5", "?page=2147483647&size=100", "?size=2147483647"})
    void invalidPagingIsRejected(String query) throws Exception {
        search("{\"filter\":{\"and\":[]}}", query)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("INVALID_PAGE"));
    }

    @Test
    void typeConfusionIsRejected() throws Exception {
        search(eq("active", "\"yes\"")).andExpect(status().isBadRequest());
        search(eq("active", "1")).andExpect(status().isBadRequest());
        search(eq("price", "\"NaN\"")).andExpect(status().isBadRequest());
        search(eq("price", "\"1e999999999\"")).andExpect(status().isBadRequest());
        search(eq("price", "1e999999999")).andExpect(status().isBadRequest());
        search(eq("id", "9223372036854775808")).andExpect(status().isBadRequest());
        search(eq("name", "{\"a\":1}")).andExpect(status().isBadRequest());
        search(eq("name", "[\"Toy Car\"]")).andExpect(status().isBadRequest());
        search(eq("category", "\"book\"")).andExpect(status().isBadRequest());
        search(eq("createdAt", "\"yesterday\"")).andExpect(status().isBadRequest());
    }

    @Test
    void conflictingKeysAreRejected() throws Exception {
        search("{\"filter\":{\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"Toy Car\",\"values\":[\"x\"]}}")
                .andExpect(status().isBadRequest());
        search("{\"filter\":{\"and\":[{\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"Toy Car\"}],"
                + "\"or\":[{\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"Clean Code\"}]}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void nullOperandsAreRejected() throws Exception {
        search(eq("name", "null")).andExpect(status().isBadRequest());
        search("{\"filter\":{\"field\":\"name\",\"operator\":\"IN\",\"values\":[\"Toy Car\",null]}}")
                .andExpect(status().isBadRequest());
        search("{\"filter\":{\"field\":\"name\",\"operator\":\"IS_NULL\",\"value\":\"x\"}}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void nulCharacterIsRejected() throws Exception {
        search(eq("name", "\"a\\u0000b\"")).andExpect(status().isBadRequest());
    }

    @Test
    void prototypePollutionKeysAreIgnored() throws Exception {
        search("{\"filter\":{\"__proto__\":{\"admin\":true},\"constructor\":{\"prototype\":{}},"
                + "\"field\":\"name\",\"operator\":\"EQ\",\"value\":\"Toy Car\"}}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(1));
    }

    @Test
    void unicodeRoundTrips() throws Exception {
        search(eq("name", "\"Ünïcödé 😀\"")).andExpect(status().isOk()).andExpect(jsonPath("$.totalHits").value(0));
    }
}
