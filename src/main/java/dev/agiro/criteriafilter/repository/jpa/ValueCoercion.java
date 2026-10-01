package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.exception.FilterTranslationException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * Coerces JSON operands (already deserialized by Jackson into String / Number /
 * Boolean) into the Java type of the target attribute, so predicates compare
 * like-typed values.
 */
final class ValueCoercion {

    /** Upper bound for integer digits and fractional digits of arbitrary-precision operands. */
    static final int MAX_NUMERIC_DIGITS = 1000;

    private ValueCoercion() {
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    static Object coerce(Object value, Class<?> targetType) {
        if (value == null) {
            return null;
        }
        if (targetType.isInstance(value) && !(value instanceof BigDecimal || value instanceof BigInteger)) {
            return value;
        }
        if ((value instanceof Map<?, ?> || value instanceof Collection<?>) && isScalar(targetType)) {
            throw new FilterTranslationException(
                    "Expected a single " + targetType.getSimpleName() + " value but got '" + value + "'");
        }
        try {
            if (targetType == String.class) {
                return value.toString();
            }
            if (targetType.isEnum()) {
                return Enum.valueOf((Class<? extends Enum>) targetType, value.toString());
            }
            if (targetType == Boolean.class || targetType == boolean.class) {
                return (value instanceof Boolean b) ? b : parseBoolean(value);
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
            return requireFinite(Double.valueOf(s), value);
        }
        if (targetType == Float.class || targetType == float.class) {
            return requireFinite(Float.valueOf(s), value);
        }
        if (targetType == BigDecimal.class) {
            return requireBounded(new BigDecimal(s), value);
        }
        if (targetType == BigInteger.class) {
            return requireBounded(new BigDecimal(s).toBigIntegerExact(), value);
        }
        return value;
    }

    private static Boolean parseBoolean(Object value) {
        String s = value.toString().trim();
        if (s.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (s.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        throw new FilterTranslationException("Cannot convert '" + value + "' to Boolean");
    }

    private static <N extends Number> N requireFinite(N number, Object original) {
        if (Double.isNaN(number.doubleValue()) || Double.isInfinite(number.doubleValue())) {
            throw new FilterTranslationException("Non-finite number '" + original + "' is not supported");
        }
        return number;
    }

    private static <N extends Number> N requireBounded(N number, Object original) {
        BigDecimal decimal = number instanceof BigInteger i ? new BigDecimal(i) : (BigDecimal) number;
        if (decimal.precision() - decimal.scale() > MAX_NUMERIC_DIGITS || decimal.scale() > MAX_NUMERIC_DIGITS) {
            throw new FilterTranslationException("Number '" + original + "' exceeds the supported precision");
        }
        return number;
    }

    private static boolean isScalar(Class<?> type) {
        return type == String.class || type.isEnum() || type == Boolean.class || type == boolean.class
                || type == UUID.class || isNumeric(type) || isTemporal(type);
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
