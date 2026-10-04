package dev.agiro.criteriafilter.annotation;

import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.model.FieldSelection;

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

    /**
     * Which fields of the annotated type are filterable. {@link FieldSelection#AUTO}
     * (the default) uses only {@code @FilterField} fields when the type has any,
     * and every field otherwise; {@link FieldSelection#ANNOTATED} and
     * {@link FieldSelection#ALL_FIELDS} force one of those behaviours.
     *
     * <p>A field with {@link FilterField#excluded() excluded = true} is always
     * left out, whatever the mode.
     */
    FieldSelection selection() default FieldSelection.AUTO;
}
