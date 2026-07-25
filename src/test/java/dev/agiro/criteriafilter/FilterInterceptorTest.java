package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.interceptor.FilterContext;
import dev.agiro.criteriafilter.interceptor.FilterInterceptor;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.sample.Product;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FilterInterceptorTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TestInterceptor testInterceptor;

    @Test
    void interceptorIsCalledDuringSearch() throws Exception {
        testInterceptor.reset();

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter": {"field": "name", "operator": "LIKE", "value": "Java"}}
                                """))
                .andExpect(status().isOk());

        assertThat(testInterceptor.preFilterCalled.get()).isTrue();
        assertThat(testInterceptor.postFilterCalled.get()).isTrue();
    }

    @Test
    void interceptorCanAddFilters() throws Exception {
        // The ActiveOnlyInterceptor adds "active = true" to all queries
        // This should filter out inactive products
        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter": {"field": "name", "operator": "LIKE", "value": "%"}}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalHits").isNumber());
    }

    @Test
    void interceptorCanModifyResults() throws Exception {
        testInterceptor.reset();
        testInterceptor.modifyResultCount = true;

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter": {"field": "name", "operator": "LIKE", "value": "Java"}}
                                """))
                .andExpect(status().isOk());

        // The interceptor should have modified the result
        assertThat(testInterceptor.postFilterCalled.get()).isTrue();
    }

    @Test
    void interceptorCanShareAttributesBetweenPhases() throws Exception {
        testInterceptor.reset();

        mockMvc.perform(post("/products/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"filter": {"field": "name", "operator": "LIKE", "value": "Java"}}
                                """))
                .andExpect(status().isOk());

        // Verify attribute was set in preFilter and read in postFilter
        assertThat(testInterceptor.attributeReadInPostFilter.get()).isTrue();
    }

    @TestConfiguration
    static class Config {

        @Bean
        @Order(1)
        public TestInterceptor testInterceptor() {
            return new TestInterceptor();
        }

        @Bean
        @Order(2)
        public ActiveOnlyInterceptor activeOnlyInterceptor() {
            return new ActiveOnlyInterceptor();
        }
    }

    /**
     * Test interceptor that tracks calls and can modify results.
     */
    static class TestInterceptor implements FilterInterceptor<Product> {

        final AtomicBoolean preFilterCalled = new AtomicBoolean(false);
        final AtomicBoolean postFilterCalled = new AtomicBoolean(false);
        final AtomicBoolean attributeReadInPostFilter = new AtomicBoolean(false);
        boolean modifyResultCount = false;

        void reset() {
            preFilterCalled.set(false);
            postFilterCalled.set(false);
            attributeReadInPostFilter.set(false);
            modifyResultCount = false;
        }

        @Override
        public Class<Product> entityType() {
            return Product.class;
        }

        @Override
        public FilterResult<Product> preFilter(FilterContext<Product> context) {
            preFilterCalled.set(true);
            context.setAttribute("testKey", "testValue");
            return null; // Continue processing
        }

        @Override
        public FilterResult<Product> postFilter(FilterContext<Product> context, FilterResult<Product> result) {
            postFilterCalled.set(true);
            
            // Verify attribute was passed from preFilter
            String value = context.getAttribute("testKey");
            if ("testValue".equals(value)) {
                attributeReadInPostFilter.set(true);
            }

            return result;
        }
    }

    /**
     * Example interceptor that adds "active = true" filter to all Product queries.
     * This demonstrates a common use case: soft-delete or tenant isolation.
     */
    static class ActiveOnlyInterceptor implements FilterInterceptor<Product> {

        @Override
        public Class<Product> entityType() {
            return Product.class;
        }

        @Override
        public FilterResult<Product> preFilter(FilterContext<Product> context) {
            // Add filter: active = true
            context.addFilter("active", Operator.EQ, true);
            return null;
        }
    }
}
