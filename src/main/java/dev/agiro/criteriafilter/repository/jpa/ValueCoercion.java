package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.exception.FilterTranslationException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.UUID;

/**
 * Coerces JSON operands (already deserialized by Jackson into String / Number /
 * Boolean) into the Java type of the target attribute, so predicates compare
 * like-typed values.
 */
final class ValueCoercion {

    private ValueCoercion() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object coerce(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value)) {
            return value;
        }
        try {
            if (targetType == String.class) {
                return value.toString();
            }
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) targetType, value.toString());
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return (value instanceof Boolean b) ? b : Boolean.valueOf(value.toString());
            }
            if (targetType == UUID.class) {
                return UUID.fromString(value.toString());
            }
            if (isNumeric(targetType)) {
                return coerceNumber(value, targetType);
            }
            if (isTemporal(targetType)) {
                return coerceTemporal(value, targetType);
            }
        } catch (FilterTranslationException e) {
            throw e;
        } catch (RuntimeException e) {
            throw new FilterTranslationException(
                    "Cannot convert '" + value + "' to " + targetType.getSimpleName(), e);
        }
        return value;
    }

    private static Object coerceNumber(Object value, Class<?> targetType) {
        String s = value.toString().trim();
        if (targetType == Integer.class || targetType == int.class) {
            return Integer.valueOf(s);
        }
        if (targetType == Long.class || targetType == long.class) {
            return Long.valueOf(s);
        }
        if (targetType == Short.class || targetType == short.class) {
            return Short.valueOf(s);
        }
        if (targetType == Byte.class || targetType == byte.class) {
            return Byte.valueOf(s);
        }
        if (targetType == Double.class || targetType == double.class) {
            return Double.valueOf(s);
        }
        if (targetType == Float.class || targetType == float.class) {
            return Float.valueOf(s);
        }
        if (targetType == BigDecimal.class) {
            return new BigDecimal(s);
        }
        if (targetType == BigInteger.class) {
            return new BigInteger(s);
        }
        return value;
    }

    private static Object coerceTemporal(Object value, Class<?> targetType) {
        String s = value.toString();
        if (targetType == Instant.class) {
            return Instant.parse(s);
        }
        if (targetType == LocalDate.class) {
            return LocalDate.parse(s);
        }
        if (targetType == LocalDateTime.class) {
            return LocalDateTime.parse(s);
        }
        if (targetType == OffsetDateTime.class) {
            return OffsetDateTime.parse(s);
        }
        if (targetType == ZonedDateTime.class) {
            return ZonedDateTime.parse(s);
        }
        return value;
    }

    private static boolean isNumeric(Class<?> type) {
        return Number.class.isAssignableFrom(type)
                || type == int.class || type == long.class || type == short.class
                || type == byte.class || type == double.class || type == float.class;
    }

    private static boolean isTemporal(Class<?> type) {
        return Instant.class.isAssignableFrom(type)
                || LocalDate.class.isAssignableFrom(type)
                || LocalDateTime.class.isAssignableFrom(type)
                || OffsetDateTime.class.isAssignableFrom(type)
                || ZonedDateTime.class.isAssignableFrom(type);
    }
}
