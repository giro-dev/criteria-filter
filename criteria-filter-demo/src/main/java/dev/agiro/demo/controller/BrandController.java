package dev.agiro.demo.controller;

import dev.agiro.criteriafilter.annotation.EnableFilterEndpoint;
import dev.agiro.demo.entity.Brand;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Brand controller using @EnableFilterEndpoint annotation (class-level).
 * Demonstrates the annotation-based approach for automatic endpoint registration.
 * 
 * This automatically registers:
 * - POST /api/brands/search
 * - GET /api/brands/search/schema
 */
@RestController
@RequestMapping("/api/brands")
@EnableFilterEndpoint(entity = Brand.class)
@Tag(name = "Brands", description = "Brand search with criteria-filter (@EnableFilterEndpoint)")
public class BrandController {
    
    // No code needed! Endpoints are auto-registered by @EnableFilterEndpoint
}
