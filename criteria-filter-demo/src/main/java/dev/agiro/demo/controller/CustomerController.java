package dev.agiro.demo.controller;

import dev.agiro.criteriafilter.annotation.EnableFilterEndpoint;
import dev.agiro.criteriafilter.annotation.FilterSearch;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.demo.entity.Customer;
import dev.agiro.demo.interceptor.ExternalCustomerInterceptor;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Customer controller with JSONB fields for testing PostgreSQL JSON operators.
 * 
 * ## JSONB Filter Examples
 * 
 * ### JSON_EXISTS - Check if a key exists
 * ```json
 * {"field": "preferences", "operator": "JSON_EXISTS", "value": "theme"}
 * ```
 * 
 * ### JSON_PATH_EQ - Check value at JSON path
 * ```json
 * {"field": "preferences", "operator": "JSON_PATH_EQ", "values": ["theme", "dark"]}
 * ```
 * 
 * ### JSON_CONTAINS - Check if JSON contains a value
 * ```json
 * {"field": "metadata", "operator": "JSON_CONTAINS", "value": {"source": "web"}}
 * ```
 * 
 * ### JSON_ARRAY_CONTAINS - Check if JSON array contains a value
 * ```json
 * {"field": "metadata.tags", "operator": "JSON_ARRAY_CONTAINS", "value": "vip"}
 * ```
 */
@RestController
@RequestMapping("/api/customers")
@EnableFilterEndpoint(entity = Customer.class)
@Tag(name = "Customers (JSONB)", description = "Customer search with JSONB fields - test PostgreSQL JSON operators")
public class CustomerController {

    // Endpoints auto-registered by @EnableFilterEndpoint (no extra interceptors,
    // sees all customers including inactive ones - "internal" view):
    // POST /api/customers/search
    // GET /api/customers/search/schema

    /**
     * "External" search variant that opts into {@link ExternalCustomerInterceptor},
     * which is NOT global ({@code global() == false}) and therefore only runs
     * here, forcing {@code active = true} in the background. The internal
     * {@code /search} endpoint above is unaffected and can still see inactive
     * customers.
     *
     * <p>Demonstrates per-endpoint interceptor selection: same entity, two
     * search endpoints, different background filtering behavior.
     */
    @FilterSearch(entity = Customer.class, interceptors = ExternalCustomerInterceptor.class)
    @PostMapping("/external-search")
    @Operation(summary = "Search customers (external view - active only, enforced by opt-in interceptor)")
    public FilterResult<Customer> externalSearch(
            @RequestBody FilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Body ignored: executed by FilterEndpointAspect (executeDefault=true default)
        return null;
    }
}
