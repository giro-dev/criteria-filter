package dev.agiro.criteriafilter.model;

/**
 * Aggregate functions supported by {@link AggregationSpec}.
 *
 * <ul>
 *   <li>{@link #SUM}, {@link #AVG} &mdash; numeric fields only</li>
 *   <li>{@link #MIN}, {@link #MAX} &mdash; any {@link Comparable} field (numbers, strings, dates, enums...)</li>
 *   <li>{@link #COUNT} &mdash; any field (non-null values), or every row when no field is given</li>
 *   <li>{@link #COUNT_DISTINCT} &mdash; any field, distinct non-null values</li>
 * </ul>
 */
public enum AggregateFunction {
    SUM,
    AVG,
    MIN,
    MAX,
    COUNT,
    COUNT_DISTINCT;

    /** Whether the function may be used without a target field. */
    public boolean fieldOptional() {
        return this == COUNT;
    }
}
