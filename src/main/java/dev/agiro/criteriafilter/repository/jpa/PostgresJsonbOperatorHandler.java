package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.model.Operator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * JPA operator handler for PostgreSQL JSONB functions.
 *
 * <p>Supports the following operators:
 * <ul>
 *   <li>{@code JSON_CONTAINS} - {@code @>} containment</li>
 *   <li>{@code JSON_CONTAINED_BY} - {@code <@} contained by</li>
 *   <li>{@code JSON_EXISTS} - {@code ?} key exists</li>
 *   <li>{@code JSON_EXISTS_ANY} - {@code ?|} any key exists</li>
 *   <li>{@code JSON_EXISTS_ALL} - {@code ?&} all keys exist</li>
 *   <li>{@code JSON_PATH_EQ} - {@code ->>} path extraction + equals</li>
 *   <li>{@code JSON_PATH_LIKE} - {@code ->>} path extraction + like</li>
 *   <li>{@code JSON_ARRAY_CONTAINS} - array containment</li>
 *   <li>{@code JSON_ARRAY_CONTAINS_ALL} - array contains all</li>
 *   <li>{@code JSON_ARRAY_CONTAINS_ANY} - array contains any</li>
 * </ul>
 *
 * <p>Example filter:
 * <pre>{@code
 * {
 *   "field": "metadata",
 *   "operator": "JSON_CONTAINS",
 *   "value": "{\"status\": \"active\"}"
 * }
 * }</pre>
 */
public class PostgresJsonbOperatorHandler implements JpaOperatorHandler {

    private static final Set<Operator> SUPPORTED = Set.of(
            Operator.JSON_CONTAINS,
            Operator.JSON_CONTAINED_BY,
            Operator.JSON_EXISTS,
            Operator.JSON_EXISTS_ANY,
            Operator.JSON_EXISTS_ALL,
            Operator.JSON_PATH_EQ,
            Operator.JSON_PATH_LIKE,
            Operator.JSON_ARRAY_CONTAINS,
            Operator.JSON_ARRAY_CONTAINS_ALL,
            Operator.JSON_ARRAY_CONTAINS_ANY
    );

    @Override
    public Set<Operator> supportedOperators() {
        return SUPPORTED;
    }

    @Override
    public Predicate handle(Operator operator, Path<?> path, List<Object> operands,
                            FieldMetadata field, CriteriaBuilder cb) {
        return switch (operator) {
            case JSON_CONTAINS -> jsonContains(cb, path, operands);
            case JSON_CONTAINED_BY -> jsonContainedBy(cb, path, operands);
            case JSON_EXISTS -> jsonExists(cb, path, operands);
            case JSON_EXISTS_ANY -> jsonExistsAny(cb, path, operands);
            case JSON_EXISTS_ALL -> jsonExistsAll(cb, path, operands);
            case JSON_PATH_EQ -> jsonPathEquals(cb, path, operands);
            case JSON_PATH_LIKE -> jsonPathLike(cb, path, operands);
            case JSON_ARRAY_CONTAINS -> jsonArrayContains(cb, path, operands);
            case JSON_ARRAY_CONTAINS_ALL -> jsonArrayContainsAll(cb, path, operands);
            case JSON_ARRAY_CONTAINS_ANY -> jsonArrayContainsAny(cb, path, operands);
            default -> throw new FilterTranslationException(
                    "Operator " + operator + " not supported by PostgresJsonbOperatorHandler");
        };
    }

    /**
     * JSONB contains: {@code column @> '{"key": "value"}'::jsonb}
     */
    private Predicate jsonContains(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String jsonValue = toJsonString(operands.get(0));
        return cb.isTrue(
                cb.function("jsonb_contains", Boolean.class,
                        path.as(String.class),
                        cb.literal(jsonValue))
        );
    }

    /**
     * JSONB contained by: {@code column <@ '{"key": "value"}'::jsonb}
     */
    private Predicate jsonContainedBy(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String jsonValue = toJsonString(operands.get(0));
        return cb.isTrue(
                cb.function("jsonb_contained_by", Boolean.class,
                        path.as(String.class),
                        cb.literal(jsonValue))
        );
    }

    /**
     * JSONB key exists: {@code column ? 'key'}
     */
    private Predicate jsonExists(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String key = operands.get(0).toString();
        return cb.isTrue(
                cb.function("jsonb_exists", Boolean.class,
                        path.as(String.class),
                        cb.literal(key))
        );
    }

    /**
     * JSONB any key exists: {@code column ?| array['k1','k2']}
     */
    private Predicate jsonExistsAny(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String[] keys = operands.stream()
                .map(Object::toString)
                .toArray(String[]::new);
        return cb.isTrue(
                cb.function("jsonb_exists_any", Boolean.class,
                        path.as(String.class),
                        cb.literal(toPostgresArray(keys)))
        );
    }

    /**
     * JSONB all keys exist: {@code column ?& array['k1','k2']}
     */
    private Predicate jsonExistsAll(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String[] keys = operands.stream()
                .map(Object::toString)
                .toArray(String[]::new);
        return cb.isTrue(
                cb.function("jsonb_exists_all", Boolean.class,
                        path.as(String.class),
                        cb.literal(toPostgresArray(keys)))
        );
    }

    /**
     * JSONB path extraction + equals: {@code column->>'path' = 'value'}
     * Operands: [path, value]
     */
    private Predicate jsonPathEquals(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        if (operands.size() != 2) {
            throw new FilterTranslationException("JSON_PATH_EQ requires exactly 2 values: [jsonPath, value]");
        }
        String jsonPath = operands.get(0).toString();
        String value = operands.get(1).toString();

        Expression<String> extracted = extractJsonPath(cb, path, jsonPath);
        return cb.equal(extracted, value);
    }

    /**
     * JSONB path extraction + like: {@code column->>'path' LIKE '%value%'}
     * Operands: [path, pattern]
     */
    private Predicate jsonPathLike(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        if (operands.size() != 2) {
            throw new FilterTranslationException("JSON_PATH_LIKE requires exactly 2 values: [jsonPath, pattern]");
        }
        String jsonPath = operands.get(0).toString();
        String pattern = operands.get(1).toString();

        Expression<String> extracted = extractJsonPath(cb, path, jsonPath);
        return cb.like(cb.lower(extracted), "%" + pattern.toLowerCase() + "%");
    }

    /**
     * JSONB array contains single value.
     * Two modes:
     * 1. Single operand: column @> '["value"]'::jsonb (array at root)
     * 2. Two operands [path, value]: column->'path' @> '["value"]'::jsonb (nested array)
     */
    private Predicate jsonArrayContains(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        if (operands.size() == 2) {
            // Nested array: column->'path' @> '["value"]'
            String jsonPath = operands.get(0).toString();
            String value = operands.get(1).toString();
            String jsonArray = toJsonArray(List.of(value));
            
            // Extract the nested path first, then check containment
            Expression<String> nestedPath = cb.function("jsonb_extract_path", String.class,
                    path.as(String.class),
                    cb.literal(jsonPath));
            return cb.isTrue(
                    cb.function("jsonb_contains", Boolean.class,
                            nestedPath,
                            cb.literal(jsonArray))
            );
        }
        // Root array: column @> '["value"]'
        String jsonArray = toJsonArray(List.of(operands.get(0)));
        return cb.isTrue(
                cb.function("jsonb_contains", Boolean.class,
                        path.as(String.class),
                        cb.literal(jsonArray))
        );
    }

    /**
     * JSONB array contains all values: {@code column @> '["v1","v2"]'::jsonb}
     */
    private Predicate jsonArrayContainsAll(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String jsonArray = toJsonArray(operands);
        return cb.isTrue(
                cb.function("jsonb_contains", Boolean.class,
                        path.as(String.class),
                        cb.literal(jsonArray))
        );
    }

    /**
     * JSONB array contains any value - uses OR of individual contains checks.
     */
    private Predicate jsonArrayContainsAny(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        Predicate[] predicates = operands.stream()
                .map(val -> jsonArrayContains(cb, path, List.of(val)))
                .toArray(Predicate[]::new);
        return cb.or(predicates);
    }

    /**
     * Extracts a text value from JSONB using ->> operator.
     * Supports nested paths like "address.city" → column->'address'->>'city'
     */
    private Expression<String> extractJsonPath(CriteriaBuilder cb, Path<?> path, String jsonPath) {
        String[] segments = jsonPath.split("\\.");
        if (segments.length == 1) {
            // Simple path: column->>'key'
            return cb.function("jsonb_extract_path_text", String.class,
                    path.as(String.class),
                    cb.literal(segments[0]));
        } else {
            // Nested path: use jsonb_extract_path_text with variadic args
            Expression<?>[] args = new Expression<?>[segments.length + 1];
            args[0] = path.as(String.class);
            for (int i = 0; i < segments.length; i++) {
                args[i + 1] = cb.literal(segments[i]);
            }
            return cb.function("jsonb_extract_path_text", String.class, args);
        }
    }

    private String toJsonString(Object value) {
        if (value instanceof String s && (s.startsWith("{") || s.startsWith("["))) {
            return s; // Already JSON
        }
        // Wrap in quotes for simple values
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
    }

    private String toJsonArray(List<Object> values) {
        return values.stream()
                .map(v -> {
                    if (v instanceof Number) {
                        return v.toString();
                    }
                    return "\"" + v.toString().replace("\"", "\\\"") + "\"";
                })
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toPostgresArray(String[] values) {
        return java.util.Arrays.stream(values)
                .map(v -> "\"" + v.replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }
}
