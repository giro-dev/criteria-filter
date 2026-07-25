package dev.agiro.criteriafilter.metamodel;

import java.util.Map;
import java.util.Optional;

/**
 * Singleton registry of {@link EntityFilterMetadata}, populated once at startup
 * and consulted at request time without further reflection.
 */
public class FilterMetadataRegistry {

    private volatile Map<Class<?>, EntityFilterMetadata> byEntity = Map.of();
    private volatile boolean initialized = false;

    /** Freezes the metamodel. Called once by the bean initializer. */
    public void initialize(Map<Class<?>, EntityFilterMetadata> metadata) {
        this.byEntity = Map.copyOf(metadata);
        this.initialized = true;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public Optional<EntityFilterMetadata> find(Class<?> entityType) {
        return Optional.ofNullable(byEntity.get(entityType));
    }

    public EntityFilterMetadata require(Class<?> entityType) {
        EntityFilterMetadata metadata = byEntity.get(entityType);
        if (metadata == null) {
            throw new IllegalStateException(
                    "No @CriteriaFilter metadata registered for " + entityType.getName());
        }
        return metadata;
    }

    public Map<Class<?>, EntityFilterMetadata> asMap() {
        return byEntity;
    }
}
