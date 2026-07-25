package dev.agiro.criteriafilter.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables filter search endpoints on an existing Spring MVC controller class.
 *
 * <p>Auto-registers {@code POST /search} and {@code GET /search/schema}
 * relative to the class-level {@code @RequestMapping} path, without requiring
 * the controller to extend {@link dev.agiro.criteriafilter.web.AbstractFilterController}.
 *
 * <p>Example:
 * <pre>{@code
 * @RestController
 * @RequestMapping("/products")
 * @EnableFilterEndpoint(entity = Product.class)
 * public class ProductController {
 *     // other endpoints...
 * }
 * }</pre>
 *
 * <p>This registers:
 * <ul>
 *   <li>{@code POST /products/search}</li>
 *   <li>{@code GET /products/search/schema}</li>
 * </ul>
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface EnableFilterEndpoint {

    /** Entity type targeted by the filter endpoints. */
    Class<?> entity();

    /** Path segment for the search endpoint. Default: "search". */
    String searchPath() default "search";

    /** Path segment for the schema endpoint. Default: "search/schema". */
    String schemaPath() default "search/schema";

    /** Whether to register the schema endpoint. */
    boolean includeSchema() default true;
}
