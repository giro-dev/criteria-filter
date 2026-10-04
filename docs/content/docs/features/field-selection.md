---
title: Field Selection
description: Choose explicitly which fields of a @CriteriaFilter type are filterable.
weight: 5
---

By default, criteria-filter infers which fields are filterable: if the type has
at least one `@FilterField`, only annotated fields are exposed; otherwise every
field is. The `selection` attribute of `@CriteriaFilter` makes that choice
explicit.

## Modes

| `selection` | Filterable fields |
|-------------|-------------------|
| `AUTO` (default) | `ANNOTATED` if the type has at least one `@FilterField`, `ALL_FIELDS` otherwise |
| `ANNOTATED` | Only fields carrying `@FilterField`. A type with no `@FilterField` exposes no fields |
| `ALL_FIELDS` | Every non-`static`, non-synthetic field. `@FilterField` only overrides the inferred defaults (name, operators, date pattern...) |

In **every** mode, `@FilterField(excluded = true)` hides the field from the
filter surface. Under `AUTO` it also counts as a `@FilterField`, so a type whose
only annotation is `excluded = true` resolves to `ANNOTATED` and exposes nothing:
use `ALL_FIELDS` to hide one field while keeping the rest.

## Examples

Only annotated fields, even if you later remove the last `@FilterField`:

```java
@Entity
@CriteriaFilter(selection = FieldSelection.ANNOTATED)
public class Invoice {
    @Id @FilterField Long id;
    @FilterField BigDecimal total;
    String internalNote;               // not filterable
}
```

Every field, tuning one of them without hiding the rest:

```java
@Entity
@CriteriaFilter(selection = FieldSelection.ALL_FIELDS)
public class Customer {
    @Id Long id;                                            // filterable
    String name;                                            // filterable
    @FilterField(operators = {Operator.EQ}) String country; // filterable, EQ only
    @FilterField(excluded = true) String passwordHash;      // never filterable
}
```

Without `selection = ALL_FIELDS`, annotating `country` would switch `AUTO` to
`ANNOTATED` and silently hide `id` and `name`.
