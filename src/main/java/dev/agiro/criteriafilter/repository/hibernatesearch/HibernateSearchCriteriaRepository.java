package dev.agiro.criteriafilter.repository.hibernatesearch;

import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;

/**
 * Hibernate Search backend — not implemented in this MVP.
 *
 * <p>This stub is where the Hibernate Search {@code SearchPredicateFactory}
 * translation will live, reusing the same {@link EntityFilterMetadata}.
 */
public class HibernateSearchCriteriaRepository<T> implements CriteriaRepository<T> {

    private final Class<T> entityType;
    private final EntityFilterMetadata metadata;

    public HibernateSearchCriteriaRepository(Class<T> entityType, EntityFilterMetadata metadata) {
        this.entityType = entityType;
        this.metadata = metadata;
    }

    @Override
    public FilterResult<T> filter(FilterRequest request, PageRequest page) {
        throw new UnsupportedOperationException(
                "Hibernate Search backend is not implemented yet (MVP is JPA-only)");
    }

    @Override
    public Class<T> entityType() {
        return entityType;
    }

    protected EntityFilterMetadata metadata() {
        return metadata;
    }
}
