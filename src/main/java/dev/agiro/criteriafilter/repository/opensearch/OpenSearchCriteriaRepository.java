package dev.agiro.criteriafilter.repository.opensearch;

import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;

/**
 * OpenSearch backend — not implemented in this MVP.
 *
 * <p>The metamodel already resolves per-backend field names and date patterns
 * (OpenSearch typically needs {@code epoch_millis}); this stub is where the
 * OpenSearch query DSL translation and search_after / scroll pagination will
 * live.
 */
public class OpenSearchCriteriaRepository<T> implements CriteriaRepository<T> {

    private final Class<T> entityType;
    private final EntityFilterMetadata metadata;

    public OpenSearchCriteriaRepository(Class<T> entityType, EntityFilterMetadata metadata) {
        this.entityType = entityType;
        this.metadata = metadata;
    }

    @Override
    public FilterResult<T> filter(FilterRequest request, PageRequest page) {
        throw new UnsupportedOperationException(
                "OpenSearch backend is not implemented yet (MVP is JPA-only)");
    }

    @Override
    public Class<T> entityType() {
        return entityType;
    }

    protected EntityFilterMetadata metadata() {
        return metadata;
    }
}
