package dev.agiro.criteriafilter.model;

/**
 * Comparison operators supported in a {@link FilterCondition}.
 *
 * <p>{@link #arity()} describes how many operands the operator consumes so that
 * validation is backend-agnostic.
 */
public enum Operator {
    // Standard operators
    EQ(Arity.SINGLE),
    NE(Arity.SINGLE),
    GT(Arity.SINGLE),
    GTE(Arity.SINGLE),
    LT(Arity.SINGLE),
    LTE(Arity.SINGLE),
    LIKE(Arity.SINGLE),
    IN(Arity.MULTI),
    BETWEEN(Arity.PAIR),
    IS_NULL(Arity.NONE),
    IS_NOT_NULL(Arity.NONE),

    // JSON/JSONB operators (PostgreSQL)
    /** JSONB contains: {@code column @> '{"key": "value"}'} */
    JSON_CONTAINS(Arity.SINGLE),
    /** JSONB is contained by: {@code column <@ '{"key": "value"}'} */
    JSON_CONTAINED_BY(Arity.SINGLE),
    /** JSONB key exists: {@code column ? 'key'} */
    JSON_EXISTS(Arity.SINGLE),
    /** JSONB any key exists: {@code column ?| array['k1','k2']} */
    JSON_EXISTS_ANY(Arity.MULTI),
    /** JSONB all keys exist: {@code column ?& array['k1','k2']} */
    JSON_EXISTS_ALL(Arity.MULTI),
    /** JSONB path value equals: {@code column->>'path' = 'value'} */
    JSON_PATH_EQ(Arity.PAIR),
    /** JSONB path value like: {@code column->>'path' LIKE '%value%'} */
    JSON_PATH_LIKE(Arity.PAIR),
    /** JSONB array contains value: {@code column @> '["value"]'} */
    JSON_ARRAY_CONTAINS(Arity.SINGLE),
    /** JSONB array contains all values: {@code column @> '["v1","v2"]'} */
    JSON_ARRAY_CONTAINS_ALL(Arity.MULTI),
    /** JSONB array contains any value */
    JSON_ARRAY_CONTAINS_ANY(Arity.MULTI);

    public enum Arity {
        /** No operand (e.g. {@code IS_NULL}). */
        NONE,
        /** Exactly one operand. */
        SINGLE,
        /** Exactly two operands (e.g. {@code BETWEEN}). */
        PAIR,
        /** One or more operands (e.g. {@code IN}). */
        MULTI
    }

    private final Arity arity;

    Operator(Arity arity) {
        this.arity = arity;
    }

    public Arity arity() {
        return arity;
    }
}
