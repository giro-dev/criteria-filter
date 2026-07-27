package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.annotation.EnableFilterEndpoint;
import dev.agiro.criteriafilter.annotation.FilterSchema;
import dev.agiro.criteriafilter.annotation.FilterSearch;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.sample.Product;
import dev.agiro.criteriafilter.web.FilterSchemaResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilterEndpointAnnotationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void enableFilterEndpointRegistersSearchAndSchema() throws Exception {
        mockMvc.perform(post("/annotated-products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter": {"field": "name", "operator": "LIKE", "value": "Java"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").isNumber());

        mockMvc.perform(get("/annotated-products/search/schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entity").value("Product"))
                .andExpect(jsonPath("$.fields").isArray());
    }

    @Test
    void filterSearchAnnotationExecutesDefaultFilter() throws Exception {
        mockMvc.perform(post("/custom-products/filter")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter": {"field": "active", "operator": "EQ", "value": true}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").isNumber());
    }

    @Test
    void filterSchemaAnnotationExecutesDefaultSchema() throws Exception {
        mockMvc.perform(get("/custom-products/schema"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.entity").value("Product"));
    }

    @TestConfiguration
    static class Config {

        @Bean
        public AnnotatedProductController annotatedProductController() {
            return new AnnotatedProductController();
        }

        @Bean
        public CustomFilterController customFilterController() {
            return new CustomFilterController();
        }
    }

    @RestController
    @RequestMapping("/annotated-products")
    @EnableFilterEndpoint(entity = Product.class)
    static class AnnotatedProductController {
    }

    @RestController
    @RequestMapping("/custom-products")
    static class CustomFilterController {

        @FilterSearch(entity = Product.class)
        @PostMapping("/filter")
        public FilterResult<Product> search(@RequestBody FilterRequest request,
                                            @RequestParam(defaultValue = "0") int page,
                                            @RequestParam(defaultValue = "20") int size) {
            return null; // auto-executed by the aspect
        }

        @FilterSchema(entity = Product.class)
        @GetMapping("/schema")
        public FilterSchemaResponse schema() {
            return null; // auto-executed by the aspect
        }
    }
}
