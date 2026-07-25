package dev.agiro.criteriafilter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a controller method as a filter-search endpoint.
 *
 * <p>When {@link #executeDefault()} is {@code true} (default), the method body
 * is ignored and the aspect runs the filter against the {@link #entity()}.
 * When {@code false}, the method is executed normally and the annotation only
 * documents the endpoint.
 *
 * <p>Example with automatic execution:
 * <pre>{@code
 * @FilterSearch(entity = Product.class)
 * @PostMapping("/products/filter")
 * public FilterResult<Product> filterProducts(FilterRequest req, PageRequest page) {
 *     return null; // auto-executed by the aspect
 * }
 * }</pre>
 *
 * <p>Example with custom logic:
 * <pre>{@code
 * @FilterSearch(entity = Product.class, executeDefault = false)
 * @PostMapping("/products/filter")
 * public FilterResult<Product> filterProducts(FilterRequest req, PageRequest page) {
 *     // custom pre/post processing
 *     return ...;
 * }
 * }</pre>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface FilterSearch {

    /** Entity type targeted by this search endpoint. */
    Class<?> entity();

    /** Whether the framework should execute the default filter logic. */
    boolean executeDefault() default true;
}
