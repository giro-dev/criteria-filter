package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.Operator;

import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Set;

/**
 * Immutable, fully-resolved metadata for a single filterable field. Built once
 * at startup so request-time filtering needs no reflection.
 *
 * @param datePatterns resolved date pattern per backend (empty for non-temporal
 *                     fields); backends do not necessarily share a format.
 */
public record FieldMetadata(
        String logicalName,
        String javaFieldName,
        Class<?> type,
        Set<Operator> operators,
        String openSearchField,
        String hibernateSearchField,
        boolean nested,
        Map<Backend, String> datePatterns,
        ChronoUnit dateTruncate
) {

    public FieldMetadata {
        operators = Set.copyOf(operators);
        datePatterns = Map.copyOf(datePatterns);
    }

    public boolean supports(Operator operator) {
        return operators.contains(operator);
    }

    /** Field name to use when querying the given backend. */
    public String fieldFor(Backend backend) {
        return switch (backend) {
            case JPA -> javaFieldName;
            case OPENSEARCH -> openSearchField;
            case HIBERNATE_SEARCH -> hibernateSearchField;
        };
    }
}
