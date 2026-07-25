package dev.agiro.criteriafilter.interceptor;

import dev.agiro.criteriafilter.repository.FilterResult;

/**
 * Interceptor for filter operations, allowing pre/post processing of filter requests
 * and results. Useful for:
 * <ul>
 *   <li>Adding business-logic filters (e.g., tenant isolation, soft-delete)</li>
 *   <li>Enforcing security constraints</li>
 *   <li>Logging and auditing</li>
 *   <li>Transforming results</li>
 * </ul>
 *
 * <p>Interceptors are executed in order (by {@link org.springframework.core.annotation.Order}).
 * The chain can be short-circuited by returning a result from {@link #preFilter}.
 *
 * @param <T> Entity type this interceptor applies to
 */
public interface FilterInterceptor<T> {

    /**
     * Returns the entity type this interceptor applies to.
     * Return {@code Object.class} to apply to all entities.
     */
    default Class<T> entityType() {
        @SuppressWarnings("unchecked")
        Class<T> type = (Class<T>) Object.class;
        return type;
    }

    /**
     * Called before the filter query is executed.
     *
     * @param context Mutable context containing the filter request and metadata
     * @return {@code null} to continue, or a {@link FilterResult} to short-circuit
     */
    default FilterResult<T> preFilter(FilterContext<T> context) {
        return null;
    }

    /**
     * Called after the filter query is executed.
     *
     * @param context Context containing the original request and result
     * @param result  The result from the repository (or previous interceptor)
     * @return The (possibly modified) result
     */
    default FilterResult<T> postFilter(FilterContext<T> context, FilterResult<T> result) {
        return result;
    }
}
