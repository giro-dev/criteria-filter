package dev.agiro.criteriafilter.interceptor;

import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.ArrayList;
import java.util.List;

/**
 * Executes the interceptor chain around a filter operation.
 * Interceptors are sorted by {@link org.springframework.core.annotation.Order}.
 */
public class FilterInterceptorChain {

    private static final Logger log = LoggerFactory.getLogger(FilterInterceptorChain.class);

    private final List<FilterInterceptor<?>> interceptors;

    public FilterInterceptorChain(List<FilterInterceptor<?>> interceptors) {
        this.interceptors = new ArrayList<>(interceptors);
        this.interceptors.sort(AnnotationAwareOrderComparator.INSTANCE);
    }

    /**
     * Executes the filter operation with all globally-applicable interceptors.
     *
     * @param entityType  The entity type being filtered
     * @param request     The original filter request
     * @param pageRequest Pagination parameters
     * @param repository  The repository to execute the query
     * @return The filter result (possibly modified by interceptors)
     */
    public <T> FilterResult<T> execute(Class<T> entityType,
                                        FilterRequest request,
                                        PageRequest pageRequest,
                                        CriteriaRepository<T> repository) {
        return execute(entityType, request, pageRequest, repository, List.of());
    }

    /**
     * Executes the filter operation with all globally-applicable interceptors,
     * plus any non-global (opt-in) interceptors explicitly requested by class.
     *
     * @param entityType           The entity type being filtered
     * @param request              The original filter request
     * @param pageRequest          Pagination parameters
     * @param repository           The repository to execute the query
     * @param extraInterceptors    Interceptor classes to include for this call
     *                             even if {@link FilterInterceptor#global()} is
     *                             {@code false}. Matched by
     *                             {@code Class#isAssignableFrom} against the
     *                             actual interceptor bean class, so interfaces
     *                             or superclasses may be used too.
     * @return The filter result (possibly modified by interceptors)
     */
    @SuppressWarnings("rawtypes")
    public <T> FilterResult<T> execute(Class<T> entityType,
                                        FilterRequest request,
                                        PageRequest pageRequest,
                                        CriteriaRepository<T> repository,
                                        List<Class<? extends FilterInterceptor>> extraInterceptors) {

        FilterContext<T> context = new FilterContext<>(entityType, request, pageRequest);
        List<FilterInterceptor<T>> applicable = findApplicable(entityType, extraInterceptors);

        // Pre-filter phase
        for (FilterInterceptor<T> interceptor : applicable) {
            try {
                FilterResult<T> shortCircuit = interceptor.preFilter(context);
                if (shortCircuit != null) {
                    log.debug("Interceptor {} short-circuited filter for {}",
                            interceptor.getClass().getSimpleName(), entityType.getSimpleName());
                    return shortCircuit;
                }
            } catch (Exception e) {
                log.error("Error in preFilter interceptor {}: {}",
                        interceptor.getClass().getSimpleName(), e.getMessage(), e);
                throw e;
            }
        }

        // Execute the actual query with merged filters
        FilterRequest finalRequest = context.buildFinalRequest();
        FilterResult<T> result = repository.filter(finalRequest, context.pageRequest());

        // Post-filter phase (reverse order)
        for (int i = applicable.size() - 1; i >= 0; i--) {
            FilterInterceptor<T> interceptor = applicable.get(i);
            try {
                result = interceptor.postFilter(context, result);
            } catch (Exception e) {
                log.error("Error in postFilter interceptor {}: {}",
                        interceptor.getClass().getSimpleName(), e.getMessage(), e);
                throw e;
            }
        }

        return result;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private <T> List<FilterInterceptor<T>> findApplicable(Class<T> entityType,
                                                            List<Class<? extends FilterInterceptor>> extraInterceptors) {
        List<FilterInterceptor<T>> applicable = new ArrayList<>();
        for (FilterInterceptor<?> interceptor : interceptors) {
            Class<?> targetType = interceptor.entityType();
            boolean matchesEntity = targetType == Object.class || targetType.isAssignableFrom(entityType);
            if (!matchesEntity) {
                continue;
            }
            boolean autoApply = interceptor.global();
            boolean explicitlyRequested = !autoApply && matchesByClass(extraInterceptors, interceptor.getClass());
            if (autoApply || explicitlyRequested) {
                applicable.add((FilterInterceptor<T>) interceptor);
            }
        }
        return applicable;
    }

    @SuppressWarnings("rawtypes")
    private boolean matchesByClass(List<Class<? extends FilterInterceptor>> extraInterceptors,
                                    Class<?> actualClass) {
        if (extraInterceptors == null || extraInterceptors.isEmpty()) {
            return false;
        }
        for (Class<? extends FilterInterceptor> requested : extraInterceptors) {
            if (requested.isAssignableFrom(actualClass)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the number of registered interceptors.
     */
    public int size() {
        return interceptors.size();
    }
}
