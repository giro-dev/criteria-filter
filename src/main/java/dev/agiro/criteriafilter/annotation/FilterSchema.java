package dev.agiro.criteriafilter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a filter-schema endpoint.
 *
 * <p>When {@link #executeDefault()} is {@code true} (default), the method body
 * is ignored and the aspect returns the schema for the {@link #entity()}.
 * When {@code false}, the method is executed normally.
 *
 * <p>Example:
 * <pre>{@code
 * @FilterSchema(entity = Product.class)
 * @GetMapping("/products/schema")
 * public FilterSchemaResponse schema() {
 *     return null; // auto-executed by the aspect
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FilterSchema {

    /** Entity type targeted by this schema endpoint. */
    Class<?> entity();

    /** Whether the framework should execute the default schema logic. */
    boolean executeDefault() default true;
}
