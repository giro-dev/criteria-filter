package dev.agiro.criteriafilter.annotation;

import dev.agiro.criteriafilter.model.Backend;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a type as filterable and declares the {@link Backend} its filter
 * requests are translated against.
 *
 * <p>May annotate the JPA entity directly, or a dedicated DTO via
 * {@link #entity()} when the filter surface differs from the persisted type.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CriteriaFilter {

    /** Entity the annotated type maps to. {@code Void.class} means the annotated type itself. */
    Class<?> entity() default Void.class;

    /** Backend fixed for this entity. */
    Backend backend() default Backend.JPA;
}
