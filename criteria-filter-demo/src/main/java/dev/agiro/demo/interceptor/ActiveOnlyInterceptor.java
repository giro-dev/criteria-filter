package dev.agiro.demo.interceptor;

import dev.agiro.criteriafilter.interceptor.FilterContext;
import dev.agiro.criteriafilter.interceptor.FilterInterceptor;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.demo.entity.Product;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Example interceptor that adds "active = true" filter to all Product queries.
 * Demonstrates soft-delete or active-only filtering pattern.
 */
@Component
@Order(1)
public class ActiveOnlyInterceptor implements FilterInterceptor<Product> {

    private static final Logger log = LoggerFactory.getLogger(ActiveOnlyInterceptor.class);

    @Override
    public Class<Product> entityType() {
        return Product.class;
    }

    @Override
    public FilterResult<Product> preFilter(FilterContext<Product> context) {
        log.info("ActiveOnlyInterceptor: Adding 'active = true' filter to Product query");
        context.addFilter("active", Operator.EQ, true);
        return null; // Continue processing
    }

    @Override
    public FilterResult<Product> postFilter(FilterContext<Product> context, FilterResult<Product> result) {
        log.info("ActiveOnlyInterceptor: Query returned {} results", result.totalHits());
        return result;
    }
}
