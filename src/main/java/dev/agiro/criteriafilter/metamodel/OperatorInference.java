package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.model.Operator;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.EnumSet;
import java.util.Set;

/**
 * Resolves the default set of operators for a field from its Java type, as
 * described in the design's inference table.
 */
public final class OperatorInference {

    private OperatorInference() {
    }

    public static Set<Operator> defaultsFor(Class<?> type) {
        if (CharSequence.class.isAssignableFrom(type)) {
            return EnumSet.of(Operator.EQ, Operator.NE, Operator.LIKE, Operator.IN,
                    Operator.IS_NULL, Operator.IS_NOT_NULL);
        }
        if (isNumber(type)) {
            return EnumSet.of(Operator.EQ, Operator.NE, Operator.GT, Operator.GTE,
                    Operator.LT, Operator.LTE, Operator.BETWEEN, Operator.IN,
                    Operator.IS_NULL, Operator.IS_NOT_NULL);
        }
        if (isTemporal(type)) {
            return EnumSet.of(Operator.EQ, Operator.NE, Operator.GT, Operator.GTE,
                    Operator.LT, Operator.LTE, Operator.BETWEEN,
                    Operator.IS_NULL, Operator.IS_NOT_NULL);
        }
        if (type == Boolean.class || type == boolean.class || type.isEnum()) {
            return EnumSet.of(Operator.EQ, Operator.NE, Operator.IN,
                    Operator.IS_NULL, Operator.IS_NOT_NULL);
        }
        // Fallback: equality only.
        return EnumSet.of(Operator.EQ, Operator.NE, Operator.IS_NULL, Operator.IS_NOT_NULL);
    }

    public static boolean isNumber(Class<?> type) {
        return Number.class.isAssignableFrom(type)
                || type == int.class || type == long.class || type == short.class
                || type == byte.class || type == double.class || type == float.class;
    }

    public static boolean isTemporal(Class<?> type) {
        return Instant.class.isAssignableFrom(type)
                || LocalDate.class.isAssignableFrom(type)
                || LocalDateTime.class.isAssignableFrom(type)
                || OffsetDateTime.class.isAssignableFrom(type)
                || ZonedDateTime.class.isAssignableFrom(type)
                || Date.class.isAssignableFrom(type);
    }
}
