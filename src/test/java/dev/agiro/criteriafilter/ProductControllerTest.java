package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.sample.Product.Category;
import dev.agiro.criteriafilter.sample.ProductJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductJpaRepository products;

    @BeforeEach
    void seed() {
        products.deleteAll();
        products.saveAll(List.of(
                new Product(1L, "Clean Code", new BigDecimal("35.00"), Category.BOOK,
                        Instant.parse("2024-01-10T00:00:00Z"), true),
                new Product(2L, "Effective Java", new BigDecimal("45.00"), Category.BOOK,
                        Instant.parse("2024-03-15T00:00:00Z"), true)
        ));
    }

    @Test
    void searchDeserializesJsonTreeAndReturnsMatches() throws Exception {
        String body = """
                {
                  "filter": {
                    "combinator": "AND",
                    "filters": [
                      {"field": "name", "operator": "LIKE", "value": "java"},
                      {"field": "category", "operator": "EQ", "value": "BOOK"}
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Effective Java"))
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void unknownFieldYields400WithUniformBody() throws Exception {
        String body = """
                { "filter": {"field": "missing", "operator": "EQ", "value": "x"} }
                """;

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("UNKNOWN_FIELD"))
                .andExpect(jsonPath("$.field").value("missing"));
    }
}
