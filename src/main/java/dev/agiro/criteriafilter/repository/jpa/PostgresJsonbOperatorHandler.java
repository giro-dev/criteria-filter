package dev.agiro.criteriafilter.repository.jpa;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.model.Operator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.hibernate.query.criteria.HibernateCriteriaBuilder;

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

    private static final ObjectMapper JSON = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

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

    private Predicate jsonContains(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        return jsonbOperator(cb, path, "@>", cb.literal(toJsonDocument(operands.get(0))));
    }

    private Predicate jsonContainedBy(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        return jsonbOperator(cb, path, "<@", cb.literal(toJsonDocument(operands.get(0))));
    }

    private Predicate jsonExists(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        String key = operands.get(0).toString();
        return cb.isTrue(cb.function("jsonb_exists", Boolean.class, path, cb.literal(key)));
    }

    private Predicate jsonExistsAny(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        return cb.isTrue(cb.function("jsonb_exists_any", Boolean.class,
                path, cb.literal(toPostgresArray(operands))));
    }

    private Predicate jsonExistsAll(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        return cb.isTrue(cb.function("jsonb_exists_all", Boolean.class,
                path, cb.literal(toPostgresArray(operands))));
    }

    private Predicate jsonPathEquals(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        if (operands.size() != 2) {
            throw new FilterTranslationException("JSON_PATH_EQ requires exactly 2 values: [jsonPath, value]");
        }
        String jsonPath = operands.get(0).toString();
        String value = operands.get(1).toString();
        return cb.equal(extractJsonPath(cb, path, jsonPath, "jsonb_extract_path_text"), value);
    }

    private Predicate jsonPathLike(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        if (operands.size() != 2) {
            throw new FilterTranslationException("JSON_PATH_LIKE requires exactly 2 values: [jsonPath, pattern]");
        }
        String jsonPath = operands.get(0).toString();
        String pattern = operands.get(1).toString();
        Expression<String> extracted = extractJsonPath(cb, path, jsonPath, "jsonb_extract_path_text");
        return cb.like(cb.lower(extracted), LikePatterns.containsIgnoreCase(pattern), LikePatterns.ESCAPE);
    }

    private Predicate jsonArrayContains(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        if (operands.size() == 2) {
            // Nested array: column->'path' @> '["value"]'
            Expression<String> nested = extractJsonPath(cb, path, operands.get(0).toString(), "jsonb_extract_path");
            return jsonbOperator(cb, nested, "@>", cb.literal(toJson(List.of(operands.get(1)))));
        }
        // Root array: column @> '["value"]'
        return jsonbOperator(cb, path, "@>", cb.literal(toJson(List.of(operands.get(0)))));
    }

    private Predicate jsonArrayContainsAll(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        return jsonbOperator(cb, path, "@>", cb.literal(toJson(operands)));
    }

    private Predicate jsonArrayContainsAny(CriteriaBuilder cb, Path<?> path, List<Object> operands) {
        Predicate[] predicates = operands.stream()
                .map(val -> jsonArrayContains(cb, path, List.of(val)))
                .toArray(Predicate[]::new);
        return cb.or(predicates);
    }

    private Expression<String> extractJsonPath(CriteriaBuilder cb, Path<?> path, String jsonPath,
                                               String function) {
        String[] segments = jsonPath.split("\\.");
        Expression<?>[] args = new Expression<?>[segments.length + 1];
        args[0] = path;
        for (int i = 0; i < segments.length; i++) {
            args[i + 1] = cb.literal(segments[i]);
        }
        return cb.function(function, String.class, args);
    }

    /**
     * Strings that look like a JSON object/array are parsed (and rejected if
     * malformed); any other value is serialized as a JSON document.
     */
    private String toJsonDocument(Object value) {
        if (value instanceof String s && (s.startsWith("{") || s.startsWith("["))) {
            try {
                return JSON.readTree(s).toString();
            } catch (JsonProcessingException e) {
                throw new FilterTranslationException("Invalid JSON value: " + s, e);
            }
        }
        return toJson(value);
    }

    private String toJson(Object value) {
        try {
            return JSON.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new FilterTranslationException("Cannot serialize value as JSON: " + value, e);
        }
    }

    private String toPostgresArray(List<Object> values) {
        return values.stream()
                .map(v -> "\"" + v.toString().replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private Predicate jsonbOperator(CriteriaBuilder cb, Expression<?> left, String operator, Expression<String> right) {
        HibernateCriteriaBuilder hibernateCriteriaBuilder = requireHibernateCriteriaBuilder(cb);
        return hibernateCriteriaBuilder.isTrue(
                hibernateCriteriaBuilder.sql("(cast(? as jsonb) " + operator + " cast(? as jsonb))",
                        Boolean.class, left, right)
        );
    }

    private HibernateCriteriaBuilder requireHibernateCriteriaBuilder(CriteriaBuilder cb) {
        if (cb instanceof HibernateCriteriaBuilder hibernateCriteriaBuilder) {
            return hibernateCriteriaBuilder;
        }
        throw new FilterTranslationException(
                "PostgresJsonbOperatorHandler requires Hibernate CriteriaBuilder for JSONB containment operators");
    }
}
