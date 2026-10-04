package dev.agiro.criteriafilter.model;

/**
 * Decides which fields of a {@link dev.agiro.criteriafilter.annotation.CriteriaFilter}
 * type are filterable. In every mode, a field marked
 * {@link dev.agiro.criteriafilter.annotation.FilterField#excluded() excluded = true}
 * is never filterable.
 */
public enum FieldSelection {

    /**
     * Inferred from the type: behaves as {@link #ANNOTATED} if at least one field
     * carries {@code @FilterField}, as {@link #ALL_FIELDS} otherwise.
     */
    AUTO,

    /** Only fields annotated with {@code @FilterField} are filterable. */
    ANNOTATED,

    /**
     * Every non-static, non-synthetic field is filterable, annotated or not;
     * {@code @FilterField} only overrides the inferred defaults.
     */
    ALL_FIELDS
}
