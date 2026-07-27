package dev.agiro.demo.interceptor;

import dev.agiro.criteriafilter.interceptor.FilterContext;
import dev.agiro.criteriafilter.interceptor.FilterInterceptor;
import dev.agiro.criteriafilter.repository.FilterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Example interceptor that logs all filter queries for auditing purposes.
 * Applies to ALL entities (entityType = Object.class).
 */
@Component
@Order(0) // Runs first
public class AuditLogInterceptor implements FilterInterceptor<Object> {

    private static final Logger log = LoggerFactory.getLogger(AuditLogInterceptor.class);

    @Override
    public Class<Object> entityType() {
        return Object.class; // Applies to all entities
    }

    @Override
    public FilterResult<Object> preFilter(FilterContext<Object> context) {
        log.info("=== AUDIT: Filter query started ===");
        log.info("Entity: {}", context.entityType().getSimpleName());
        log.info("Filter: {}", context.request().filter());
        log.info("Page: {}, Size: {}", context.pageRequest().page(), context.pageRequest().size());
        
        // Store start time for duration calculation
        context.setAttribute("startTime", System.currentTimeMillis());
        
        return null; // Continue processing
    }

    @Override
    public FilterResult<Object> postFilter(FilterContext<Object> context, FilterResult<Object> result) {
        Long startTime = context.getAttribute("startTime");
        long duration = startTime != null ? System.currentTimeMillis() - startTime : 0;
        
        log.info("=== AUDIT: Filter query completed ===");
        log.info("Entity: {}", context.entityType().getSimpleName());
        log.info("Results: {} total hits, {} returned", result.totalHits(), result.content().size());
        log.info("Duration: {} ms", duration);
        
        return result;
    }
}
