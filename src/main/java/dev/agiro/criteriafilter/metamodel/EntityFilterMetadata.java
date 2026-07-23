package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.exception.UnknownFieldException;
import dev.agiro.criteriafilter.model.Backend;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Immutable metamodel for one filterable entity: its backend and the resolved
 * metadata of every exposed field, keyed by logical name.
 */
public record EntityFilterMetadata(
        Class<?> entityType,
        Backend backend,
        Map<String, FieldMetadata> fields
) {

    public EntityFilterMetadata {
        fields = Map.copyOf(fields);
    }

    public Optional<FieldMetadata> find(String logicalName) {
        return Optional.ofNullable(fields.get(logicalName));
    }

    /** Resolves a field or fails fast with {@link UnknownFieldException}. */
    public FieldMetadata require(String logicalName) {
        FieldMetadata field = fields.get(logicalName);
        if (field == null) {
            throw new UnknownFieldException(logicalName);
        }
        return field;
    }

    public Collection<FieldMetadata> allFields() {
        return fields.values();
    }
}
