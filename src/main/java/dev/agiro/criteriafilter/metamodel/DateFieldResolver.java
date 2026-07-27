package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.model.Backend;

/**
 * Extension point for resolving the date/time pattern of custom temporal types.
 * Register beans of this type to teach the metamodel how to format types not
 * covered by the built-in inference.
 */
public interface DateFieldResolver {

    /** Whether this resolver handles the given field type. */
    boolean supports(Class<?> type);

    /**
     * Pattern used to (de)serialize the value for the given backend, or
     * {@code null} to fall back to the next resolver / default.
     */
    String pattern(Class<?> type, Backend backend);
}
