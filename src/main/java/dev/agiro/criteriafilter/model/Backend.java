package dev.agiro.criteriafilter.model;

/**
 * Persistence backend a query is translated against. The backend is fixed per
 * entity and declared through {@link dev.agiro.criteriafilter.annotation.CriteriaFilter}.
 */
public enum Backend {
    JPA,
    OPENSEARCH,
    HIBERNATE_SEARCH
}
