package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.model.Backend;

import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves the date/time pattern of a temporal field, per backend, following
 * the design's priority order:
 * <ol>
 *     <li>explicit {@code @FilterField(datePattern=...)};</li>
 *     <li>a registered {@link DateFieldResolver} for custom types;</li>
 *     <li>inference from the Java type.</li>
 * </ol>
 *
 * <p>Patterns are resolved independently per backend because OpenSearch usually
 * needs {@code epoch_millis} or an index-specific format rather than the JPA one.
 */
public class DatePatternResolver {

    private static final String ISO_INSTANT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    private static final String ISO_OFFSET = "yyyy-MM-dd'T'HH:mm:ssXXX";
    private static final String ISO_DATE = "yyyy-MM-dd";
    private static final String OPENSEARCH_TEMPORAL = "epoch_millis";

    private final List<DateFieldResolver> resolvers;
    private final String defaultDateTimePattern;

    public DatePatternResolver(List<DateFieldResolver> resolvers, String defaultDateTimePattern) {
        this.resolvers = List.copyOf(resolvers);
        this.defaultDateTimePattern = defaultDateTimePattern;
    }

    /**
     * @return per-backend pattern map, or an empty map when the type is not temporal.
     */
    public Map<Backend, String> resolve(Class<?> type, String explicitPattern) {
        if (!OperatorInference.isTemporal(type)) {
            return Map.of();
        }
        Map<Backend, String> patterns = new EnumMap<>(Backend.class);
        for (Backend backend : Backend.values()) {
            patterns.put(backend, patternFor(type, backend, explicitPattern));
        }
        return patterns;
    }

    private String patternFor(Class<?> type, Backend backend, String explicitPattern) {
        if (explicitPattern != null && !explicitPattern.isBlank()) {
            return explicitPattern;
        }
        for (DateFieldResolver resolver : resolvers) {
            if (resolver.supports(type)) {
                String resolved = resolver.pattern(type, backend);
                if (resolved != null && !resolved.isBlank()) {
                    return resolved;
                }
            }
        }
        return inferred(type, backend);
    }

    private String inferred(Class<?> type, Backend backend) {
        if (backend == Backend.OPENSEARCH) {
            return LocalDate.class.isAssignableFrom(type) ? ISO_DATE : OPENSEARCH_TEMPORAL;
        }
        if (Instant.class.isAssignableFrom(type)) {
            return ISO_INSTANT;
        }
        if (LocalDate.class.isAssignableFrom(type)) {
            return ISO_DATE;
        }
        if (OffsetDateTime.class.isAssignableFrom(type) || ZonedDateTime.class.isAssignableFrom(type)) {
            return ISO_OFFSET;
        }
        // LocalDateTime / java.util.Date / Timestamp: no inherent offset.
        return defaultDateTimePattern;
    }
}
