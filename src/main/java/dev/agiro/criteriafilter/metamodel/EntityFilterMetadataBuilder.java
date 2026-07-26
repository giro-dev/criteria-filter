package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.Operator;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Builds an immutable {@link EntityFilterMetadata} for a {@code @CriteriaFilter}
 * type. Reflection happens here, exactly once per type at startup.
 */
public class EntityFilterMetadataBuilder {

    private final DatePatternResolver datePatternResolver;

    public EntityFilterMetadataBuilder(DatePatternResolver datePatternResolver) {
        this.datePatternResolver = datePatternResolver;
    }

    public EntityFilterMetadata build(Class<?> annotatedType) {
        CriteriaFilter marker = annotatedType.getAnnotation(CriteriaFilter.class);
        if (marker == null) {
            throw new IllegalArgumentException(
                    annotatedType.getName() + " is not annotated with @CriteriaFilter");
        }
        Class<?> entityType = marker.entity() == Void.class ? annotatedType : marker.entity();

        Map<String, FieldMetadata> fields = new LinkedHashMap<>();
        boolean hasExplicitFields = hasAnyFilterFieldAnnotation(annotatedType);
        
        for (Field field : declaredFields(annotatedType)) {
            FilterField ff = field.getAnnotation(FilterField.class);
            
            // If explicit @FilterField annotations exist, only include annotated fields
            if (hasExplicitFields) {
                if (ff == null || ff.excluded()) {
                    continue;
                }
            } else {
                // No explicit annotations: include all non-synthetic, non-static fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) ||
                    field.isSynthetic() ||
                    (ff != null && ff.excluded())) {
                    continue;
                }
            }
            
            FieldMetadata metadata = buildField(annotatedType, entityType, field, ff);
            FieldMetadata previous = fields.putIfAbsent(metadata.logicalName(), metadata);
            if (previous != null) {
                throw new IllegalStateException("Duplicate logical filter field '"
                        + metadata.logicalName() + "' on " + annotatedType.getName());
            }
        }
        return new EntityFilterMetadata(entityType, marker.backend(), fields);
    }
    
    private boolean hasAnyFilterFieldAnnotation(Class<?> type) {
        for (Field field : declaredFields(type)) {
            if (field.getAnnotation(FilterField.class) != null) {
                return true;
            }
        }
        return false;
    }

    private FieldMetadata buildField(Class<?> annotatedType, Class<?> entityType,
                                     Field field, FilterField ff) {
        String javaFieldName = field.getName();

        // Fail-fast: a @FilterField on a DTO must map to a real entity attribute.
        if (entityType != annotatedType && findField(entityType, javaFieldName) == null) {
            throw new IllegalStateException("Filter field '" + javaFieldName + "' declared on "
                    + annotatedType.getName() + " does not exist on entity " + entityType.getName());
        }

        // Handle case when no @FilterField annotation (auto-discovery mode)
        String logicalName = (ff == null || ff.name().isBlank()) ? javaFieldName : ff.name();
        boolean explicitJson = ff != null && ff.json();
        Set<Operator> operators;
        if (ff != null && ff.operators().length > 0) {
            operators = EnumSet.copyOf(java.util.Arrays.asList(ff.operators()));
        } else if (explicitJson) {
            // @FilterField(json = true) forces JSON operators regardless of Java type
            // (e.g. a POJO persisted as JSON/JSONB rather than a generic Map).
            operators = OperatorInference.jsonDefaults();
        } else {
            operators = OperatorInference.defaultsFor(field.getType());
        }
        String openSearchField = (ff == null || ff.openSearchField().isBlank()) ? logicalName : ff.openSearchField();
        String hibernateSearchField = (ff == null || ff.hibernateSearchField().isBlank())
                ? logicalName : ff.hibernateSearchField();
        String datePattern = (ff == null) ? "" : ff.datePattern();
        Map<Backend, String> datePatterns = datePatternResolver.resolve(field.getType(), datePattern);
        boolean nested = (ff != null) && ff.nested();
        java.time.temporal.ChronoUnit dateTruncate = (ff == null) 
                ? java.time.temporal.ChronoUnit.MILLIS : ff.dateTruncate();

        return new FieldMetadata(logicalName, javaFieldName, field.getType(), operators,
                openSearchField, hibernateSearchField, nested, datePatterns, dateTruncate);
    }

    private static Iterable<Field> declaredFields(Class<?> type) {
        Map<String, Field> byName = new LinkedHashMap<>();
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                byName.putIfAbsent(f.getName(), f);
            }
        }
        return byName.values();
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
            try {
                return c.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // continue up the hierarchy
            }
        }
        return null;
    }
}
