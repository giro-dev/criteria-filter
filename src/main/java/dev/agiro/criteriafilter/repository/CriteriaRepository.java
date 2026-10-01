package dev.agiro.criteriafilter.repository;

import dev.agiro.criteriafilter.model.AggregationRequest;
import dev.agiro.criteriafilter.model.FilterRequest;

/**
 * Backend-agnostic contract for running a {@link FilterRequest}. One
 * implementation per backend ({@code JpaCriteriaRepository}, ...); the concrete
 * backend is fixed per entity and never branched on by client code.
 */
public interface CriteriaRepository<T> {

    FilterResult<T> filter(FilterRequest request, PageRequest page);

    /**
     * Runs a grouped aggregation over the entities matching the request filter.
     * The request is expected to be validated already ({@code FilterValidator}).
     *
     * @throws UnsupportedOperationException if the backend does not support aggregations
     */
    default AggregationResult aggregate(AggregationRequest request) {
        throw new UnsupportedOperationException(
                "Aggregations are not supported by " + getClass().getSimpleName());
    }

    /** Entity type this repository queries. */
    Class<T> entityType();
}
