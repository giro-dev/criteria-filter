package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.metamodel.FieldMetadata;
import dev.agiro.criteriafilter.model.Operator;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;

import java.util.List;
import java.util.Set;

/**
 * Strategy interface for handling specific operators in JPA queries.
 *
 * <p>Implementations can be registered with {@link JpaSpecificationTranslator}
 * to extend the set of supported operators, e.g., for database-specific
 * functions like PostgreSQL JSONB operators.
 *
 * <p>Example usage:
 * <pre>{@code
 * @Bean
 * public JpaOperatorHandler jsonbContainsHandler() {
 *     return new PostgresJsonbContainsHandler();
 * }
 * }</pre>
 */
@FunctionalInterface
public interface JpaOperatorHandler {

    /**
     * Returns the set of operators this handler supports.
     * Default implementation returns empty set; override for non-functional usage.
     */
    default Set<Operator> supportedOperators() {
        return Set.of();
    }

    /**
     * Builds a JPA {@link Predicate} for the given operator and operands.
     *
     * @param operator the operator to handle
     * @param path     the JPA path to the field
     * @param operands the coerced operand values
     * @param field    metadata about the field being filtered
     * @param cb       the JPA CriteriaBuilder
     * @return a Predicate representing the filter condition
     */
    Predicate handle(Operator operator, Path<?> path, List<Object> operands,
                     FieldMetadata field, CriteriaBuilder cb);
}
