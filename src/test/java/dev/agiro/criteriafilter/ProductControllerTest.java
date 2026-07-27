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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

    @Test
    void searchWithIhoStyleNestedAndOrGroups() throws Exception {
        String body = """
                {
                  "filter": {
                    "and": [
                      {"field": "category", "operator": "EQ", "value": "BOOK"},
                      {
                        "or": [
                          {"field": "name", "operator": "LIKE", "value": "Clean"},
                          {"field": "price", "operator": "GT", "value": 40}
                        ]
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(2))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void searchWithDeeplyNestedGroups() throws Exception {
        String body = """
                {
                  "filter": {
                    "or": [
                      {
                        "and": [
                          {"field": "name", "operator": "LIKE", "value": "Effective"},
                          {"field": "price", "operator": "GTE", "value": 45}
                        ]
                      },
                      {
                        "and": [
                          {"field": "name", "operator": "LIKE", "value": "Clean"},
                          {"field": "price", "operator": "LTE", "value": 35}
                        ]
                      }
                    ]
                  }
                }
                """;

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").value(2));
    }

    @Test
    void schemaEndpointReturnsFieldsAndOperators() throws Exception {
        mockMvc.perform(get("/products/search/schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entity").value("Product"))
                .andExpect(jsonPath("$.fields").isArray())
                .andExpect(jsonPath("$.fields[?(@.name=='name')]").exists())
                .andExpect(jsonPath("$.fields[?(@.name=='price')]").exists())
                .andExpect(jsonPath("$.fields[?(@.name=='category')]").exists())
                .andExpect(jsonPath("$.operators").isArray())
                .andExpect(jsonPath("$.operators[?(@.name=='EQ')]").exists())
                .andExpect(jsonPath("$.operators[?(@.name=='LIKE')]").exists())
                .andExpect(jsonPath("$.example").exists())
                .andExpect(jsonPath("$.example.simpleAnd").exists())
                .andExpect(jsonPath("$.example.nestedAndOr").exists());
    }

    @Test
    void schemaFieldsContainSupportedOperators() throws Exception {
        mockMvc.perform(get("/products/search/schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fields[?(@.name=='name')].operators").isArray())
                .andExpect(jsonPath("$.fields[?(@.name=='name')].type").value("String"))
                .andExpect(jsonPath("$.fields[?(@.name=='price')].type").value("BigDecimal"));
    }
}
