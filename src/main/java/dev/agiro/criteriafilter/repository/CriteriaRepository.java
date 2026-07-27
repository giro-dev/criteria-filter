package dev.agiro.criteriafilter.repository;

import dev.agiro.criteriafilter.model.FilterRequest;

/**
 * Backend-agnostic contract for running a {@link FilterRequest}. One
 * implementation per backend ({@code JpaCriteriaRepository}, ...); the concrete
 * backend is fixed per entity and never branched on by client code.
 */
public interface CriteriaRepository<T> {

    FilterResult<T> filter(FilterRequest request, PageRequest page);

    /** Entity type this repository queries. */
    Class<T> entityType();
}
