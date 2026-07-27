package dev.agiro.criteriafilter.annotation;

import dev.agiro.criteriafilter.model.Operator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

/**
 * Declares a filterable field. No attribute is mandatory: every default is
 * resolved from the Java type of the field, and explicit attributes only
 * override that default point by point.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FilterField {

    /** Logical name exposed in the {@code FilterRequest}. Empty = Java field name. */
    String name() default "";

    /** Allowed operators. Empty = inferred from the field type. */
    Operator[] operators() default {};

    /** OpenSearch document field. Empty = same as the logical name. */
    String openSearchField() default "";

    /** Hibernate Search field. Empty = same as the logical name. */
    String hibernateSearchField() default "";

    /** Whether the field maps to a nested object / join. */
    boolean nested() default false;

    /**
     * Marks the field as a JSON/JSONB column, regardless of its Java type.
     * Useful when the column stores a custom Java object (POJO) serialized
     * as JSON rather than a generic {@code Map}. When {@code true} and no
     * explicit {@link #operators()} are given, JSON-specific operators
     * (e.g. {@code JSON_EXISTS}, {@code JSON_PATH_EQ}, {@code JSON_CONTAINS},
     * {@code JSON_ARRAY_CONTAINS}) are included by default.
     */
    boolean json() default false;

    /** Date/time format pattern for {@code Instant}/{@code Timestamp}/{@code LocalDate}... */
    String datePattern() default "";

    /** Truncation applied to temporal values before comparison. */
    ChronoUnit dateTruncate() default ChronoUnit.MILLIS;

    /** Hides the field from the filter surface entirely. */
    boolean excluded() default false;
}
