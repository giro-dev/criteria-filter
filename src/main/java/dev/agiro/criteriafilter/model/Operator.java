package dev.agiro.criteriafilter.model;

/**
 * Comparison operators supported in a {@link FilterCondition}.
 *
 * <p>{@link #arity()} describes how many operands the operator consumes so that
 * validation is backend-agnostic.
 */
public enum Operator {
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
    IS_NOT_NULL(Arity.NONE);

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
