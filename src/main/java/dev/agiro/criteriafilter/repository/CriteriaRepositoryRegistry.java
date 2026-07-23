package dev.agiro.criteriafilter.repository;

import java.util.Map;

/**
 * Resolves the {@link CriteriaRepository} for an entity type. Populated once at
 * startup, so client code never inspects the backend itself.
 */
public class CriteriaRepositoryRegistry {

    private volatile Map<Class<?>, CriteriaRepository<?, ?>> repositories = Map.of();

    public void initialize(Map<Class<?>, CriteriaRepository<?, ?>> repositories) {
        this.repositories = Map.copyOf(repositories);
    }

    @SuppressWarnings("unchecked")
    public <T> CriteriaRepository<T, ?> resolve(Class<T> entityType) {
        CriteriaRepository<?, ?> repository = repositories.get(entityType);
        if (repository == null) {
            throw new IllegalStateException(
                    "No CriteriaRepository registered for " + entityType.getName());
        }
        return (CriteriaRepository<T, ?>) repository;
    }
}
