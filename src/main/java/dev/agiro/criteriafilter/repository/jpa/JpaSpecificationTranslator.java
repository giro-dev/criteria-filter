package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.LogicalOperator;
import dev.agiro.criteriafilter.model.Operator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.IdentifiableType;
import jakarta.persistence.metamodel.SingularAttribute;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Translates a {@link FilterNode} tree into a Spring Data JPA
 * {@link Specification}, driven by the resolved {@link EntityFilterMetadata}.
 *
 * <p>Supports extensibility through {@link JpaOperatorHandler} for custom
 * operators like PostgreSQL JSONB functions.
 */
public class JpaSpecificationTranslator {

    private final Map<Operator, JpaOperatorHandler> handlers = new EnumMap<>(Operator.class);

    /**
     * Creates a translator with no custom handlers (standard operators only).
     */
    public JpaSpecificationTranslator() {
    }

    /**
     * Creates a translator with the given custom operator handlers.
     *
     * @param customHandlers handlers for extended operators (e.g., JSONB)
     */
    public JpaSpecificationTranslator(List<JpaOperatorHandler> customHandlers) {
        if (customHandlers != null) {
            for (JpaOperatorHandler handler : customHandlers) {
                registerHandler(handler);
            }
        }
    }

    /**
     * Registers a custom operator handler. Handlers registered later
     * override earlier ones for the same operators.
     */
    public void registerHandler(JpaOperatorHandler handler) {
        for (Operator op : handler.supportedOperators()) {
            handlers.put(op, handler);
        }
    }

    public <T> Specification<T> toSpecification(FilterNode node, EntityFilterMetadata metadata) {
        return (root, query, cb) -> toPredicate(node, metadata, root, cb);
    }

    /**
     * Builds the predicate for {@code node} directly, for use in any
     * {@code CriteriaQuery} (entity, count or tuple query).
     */
    public Predicate toPredicate(FilterNode node, EntityFilterMetadata metadata,
                                 Root<?> root, CriteriaBuilder cb) {
        return buildPredicate(node, metadata, root, cb);
    }

    /**
     * Resolves the JPA path of a field. Associations resolve to the target
     * entity's identifier, matching how conditions compare them.
     */
    public Path<?> resolveValuePath(FieldMetadata field, Root<?> root) {
        Path<?> path = resolvePath(root, field.fieldFor(Backend.JPA));
        SingularAttribute<?, ?> id = identifierOf(path);
        return id != null ? path.get(id.getName()) : path;
    }

    private Predicate buildPredicate(FilterNode node, EntityFilterMetadata metadata,
                                     Root<?> root, CriteriaBuilder cb) {
        if (node instanceof FilterGroup group) {
            List<Predicate> children = new ArrayList<>();
            for (FilterNode child : group.filters()) {
                children.add(buildPredicate(child, metadata, root, cb));
            }
            if (children.isEmpty()) {
                return group.combinator() == LogicalOperator.OR ? cb.disjunction() : cb.conjunction();
            }
            Predicate[] array = children.toArray(new Predicate[0]);
            return switch (group.combinator()) {
                case AND -> cb.and(array);
                case OR -> cb.or(array);
            };
        }
        if (node instanceof FilterCondition condition) {
            return conditionPredicate(condition, metadata, root, cb);
        }
        throw new FilterTranslationException("Unsupported filter node: " + node);
    }

    private Predicate conditionPredicate(FilterCondition condition, EntityFilterMetadata metadata,
                                         Root<?> root, CriteriaBuilder cb) {
        FieldMetadata field = metadata.require(condition.field());
        Path<?> path = resolvePath(root, field.fieldFor(Backend.JPA));
        Operator operator = condition.operator();
        List<Object> operands = condition.operands();

        // Check for custom handler first
        JpaOperatorHandler customHandler = handlers.get(operator);
        if (customHandler != null) {
            return customHandler.handle(operator, path, operands, field, cb);
        }

        if (operator == Operator.IS_NULL) {
            return cb.isNull(path);
        }
        if (operator == Operator.IS_NOT_NULL) {
            return cb.isNotNull(path);
        }

        // Associations are compared by their identifier.
        Class<?> type = field.type();
        SingularAttribute<?, ?> id = identifierOf(path);
        if (id != null) {
            path = path.get(id.getName());
            type = id.getJavaType();
        }

        // Standard operators
        return switch (operator) {
            case EQ -> cb.equal(path, coerceSingle(operands, type, operator));
            case NE -> cb.notEqual(path, coerceSingle(operands, type, operator));
            case LIKE -> like(cb, path, coerceSingle(operands, type, operator));
            case IN -> in(path, coerceAll(operands, type, operator));
            case GT -> cb.greaterThan(comparable(path), comparable(coerceSingle(operands, type, operator)));
            case GTE -> cb.greaterThanOrEqualTo(comparable(path), comparable(coerceSingle(operands, type, operator)));
            case LT -> cb.lessThan(comparable(path), comparable(coerceSingle(operands, type, operator)));
            case LTE -> cb.lessThanOrEqualTo(comparable(path), comparable(coerceSingle(operands, type, operator)));
            case BETWEEN -> between(cb, path, operands, type);
            default -> throw new FilterTranslationException(
                    "Unsupported operator: " + operator + ". Register a JpaOperatorHandler to support it.");
        };
    }

    private Predicate like(CriteriaBuilder cb, Path<?> path, Object value) {
        return cb.like(cb.lower(path.as(String.class)),
                LikePatterns.containsIgnoreCase(value.toString()), LikePatterns.ESCAPE);
    }

    @SuppressWarnings("unchecked")
    private Predicate in(Path<?> path, List<Object> values) {
        if (values.isEmpty()) {
            throw new FilterTranslationException("IN requires at least one value");
        }
        return ((Path<Object>) path).in(values);
    }

    private static SingularAttribute<?, ?> identifierOf(Path<?> path) {
        if (path.getModel() instanceof SingularAttribute<?, ?> attribute
                && attribute.getType() instanceof IdentifiableType<?> target
                && target.hasSingleIdAttribute()) {
            return target.getId(target.getIdType().getJavaType());
        }
        return null;
    }

    private Predicate between(CriteriaBuilder cb, Path<?> path, List<Object> operands, Class<?> type) {
        if (operands.size() != 2) {
            throw new FilterTranslationException("BETWEEN requires exactly 2 values, got " + operands.size());
        }
        Comparable<Object> low = comparable(ValueCoercion.coerce(operands.get(0), type));
        Comparable<Object> high = comparable(ValueCoercion.coerce(operands.get(1), type));
        return cb.between(comparable(path), low, high);
    }

    private Object coerceSingle(List<Object> operands, Class<?> type, Operator operator) {
        if (operands.size() != 1) {
            throw new FilterTranslationException(
                    operator + " requires exactly 1 value, got " + operands.size());
        }
        return ValueCoercion.coerce(operands.get(0), type);
    }

    private List<Object> coerceAll(List<Object> operands, Class<?> type, Operator operator) {
        if (operands.isEmpty()) {
            throw new FilterTranslationException(operator + " requires at least 1 value");
        }
        List<Object> coerced = new ArrayList<>(operands.size());
        for (Object operand : operands) {
            coerced.add(ValueCoercion.coerce(operand, type));
        }
        return coerced;
    }

    private static Path<?> resolvePath(Root<?> root, String attribute) {
        Path<?> path = root;
        for (String segment : attribute.split("\\.")) {
            path = path.get(segment);
        }
        return path;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Comparable comparable(Object value) {
        return (Comparable) value;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static jakarta.persistence.criteria.Expression<Comparable> comparable(Path<?> path) {
        return (jakarta.persistence.criteria.Expression<Comparable>) (jakarta.persistence.criteria.Expression<?>) path;
    }
}
