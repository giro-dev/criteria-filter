package dev.agiro.demo.controller;

import dev.agiro.criteriafilter.annotation.FilterSchema;
import dev.agiro.criteriafilter.annotation.FilterSearch;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.web.FilterSchemaResponse;
import dev.agiro.demo.entity.Order;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/**
 * Order controller using @FilterSearch and @FilterSchema annotations (method-level).
 * Demonstrates the most flexible approach with custom endpoint paths.
 */
@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order search with criteria-filter (@FilterSearch/@FilterSchema)")
public class OrderController {

    /**
     * Custom search endpoint with method-level annotation.
     * The actual filter logic is executed by the AOP aspect.
     */
    @FilterSearch(entity = Order.class)
    @PostMapping("/filter")
    @Operation(summary = "Search orders with filters")
    public FilterResult<Order> searchOrders(
            @RequestBody FilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // Method body is ignored when executeDefault=true (default)
        // The AOP aspect intercepts and executes the filter logic
        return null;
    }

    /**
     * Custom schema endpoint with method-level annotation.
     */
    @FilterSchema(entity = Order.class)
    @GetMapping("/filter/schema")
    @Operation(summary = "Get order filter schema")
    public FilterSchemaResponse getOrderSchema() {
        // Method body is ignored when executeDefault=true (default)
        return null;
    }

    /**
     * Example of a custom endpoint that uses filter but adds business logic.
     * Set executeDefault=false to execute your own logic.
     */
    @FilterSearch(entity = Order.class, executeDefault = false)
    @PostMapping("/pending")
    @Operation(summary = "Search only pending orders (custom logic)")
    public FilterResult<Order> searchPendingOrders(
            @RequestBody FilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        // With executeDefault=false, this method body IS executed
        // You can add custom logic here before/after the filter
        // For now, just return null as a placeholder
        return null;
    }
}
