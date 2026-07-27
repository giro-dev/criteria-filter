package dev.agiro.demo.interceptor;

import dev.agiro.criteriafilter.interceptor.FilterContext;
import dev.agiro.criteriafilter.interceptor.FilterInterceptor;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.demo.entity.Customer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Opt-in interceptor that restricts customer searches to active customers
 * only. Not applied automatically ({@link #global()} returns {@code false}):
 * it must be explicitly requested by an endpoint via
 * {@code @EnableFilterEndpoint(interceptors = ExternalCustomerInterceptor.class)}
 * or {@code @FilterSearch(interceptors = ExternalCustomerInterceptor.class)}.
 *
 * <p>This demonstrates per-endpoint interceptor selection: an "external"
 * search endpoint can use this interceptor, while an "internal" endpoint for
 * the same entity can skip it (or attach a different one), without either
 * affecting the other.
 */
@Component
@Order(10)
public class ExternalCustomerInterceptor implements FilterInterceptor<Customer> {

    private static final Logger log = LoggerFactory.getLogger(ExternalCustomerInterceptor.class);

    @Override
    public Class<Customer> entityType() {
        return Customer.class;
    }

    @Override
    public boolean global() {
        return false; // opt-in only
    }

    @Override
    public FilterResult<Customer> preFilter(FilterContext<Customer> context) {
        log.info("ExternalCustomerInterceptor: restricting search to active customers only");
        context.addFilter("active", Operator.EQ, true);
        return null;
    }
}
