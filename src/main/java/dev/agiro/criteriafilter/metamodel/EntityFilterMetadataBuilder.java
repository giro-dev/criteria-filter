package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.annotation.FilterField;
import dev.agiro.criteriafilter.model.Operator;

import java.lang.reflect.Field;
import java.util.Arrays;
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

        Iterable<Field> allFields = declaredFields(annotatedType);
        boolean hasExplicitFields = hasAnyFilterFieldAnnotation(allFields);

        Map<String, FieldMetadata> fields = new LinkedHashMap<>();
        for (Field field : allFields) {
            FilterField ff = field.getAnnotation(FilterField.class);
            if (shouldSkip(field, ff, hasExplicitFields)) continue;

            if (entityType != annotatedType && findField(entityType, field.getName()) == null) {
                throw new IllegalStateException("Filter field '" + field.getName() + "' declared on "
                        + annotatedType.getName() + " does not exist on entity " + entityType.getName());
            }

            FieldMetadata metadata = buildField(field, ff);
            FieldMetadata previous = fields.putIfAbsent(metadata.logicalName(), metadata);
            if (previous != null) {
                throw new IllegalStateException("Duplicate logical filter field '"
                        + metadata.logicalName() + "' on " + annotatedType.getName());
            }
        }
        return new EntityFilterMetadata(entityType, marker.backend(), fields);
    }

    private static boolean shouldSkip(Field field, FilterField ff, boolean hasExplicitFields) {
        if (hasExplicitFields) {
            return ff == null || ff.excluded();
        }
        return java.lang.reflect.Modifier.isStatic(field.getModifiers())
                || field.isSynthetic()
                || (ff != null && ff.excluded());
    }

    private static boolean hasAnyFilterFieldAnnotation(Iterable<Field> fields) {
        for (Field field : fields) {
            if (field.getAnnotation(FilterField.class) != null) return true;
        }
        return false;
    }

    private FieldMetadata buildField(Field field, FilterField ff) {
        String javaFieldName = field.getName();
        String logicalName = (ff == null || ff.name().isBlank()) ? javaFieldName : ff.name();
        return new FieldMetadata(
                logicalName,
                javaFieldName,
                field.getType(),
                inferOperators(ff, field.getType()),
                (ff == null || ff.openSearchField().isBlank()) ? logicalName : ff.openSearchField(),
                (ff == null || ff.hibernateSearchField().isBlank()) ? logicalName : ff.hibernateSearchField(),
                ff != null && ff.nested(),
                datePatternResolver.resolve(field.getType(), ff == null ? "" : ff.datePattern()),
                ff == null ? java.time.temporal.ChronoUnit.MILLIS : ff.dateTruncate());
    }

    private static Set<Operator> inferOperators(FilterField ff, Class<?> type) {
        if (ff != null && ff.operators().length > 0) {
            return EnumSet.copyOf(Arrays.asList(ff.operators()));
        }
        if (ff != null && ff.json()) {
            return OperatorInference.jsonDefaults();
        }
        return OperatorInference.defaultsFor(type);
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
