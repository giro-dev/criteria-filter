---
title: JSONB Support
description: Query PostgreSQL JSONB columns with JSON operators.
weight: 20
---

## Entity

```java
@Entity
@CriteriaFilter
public class Customer {
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    @FilterField(json = true)
    private CustomerPreferences preferences;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
}
```

Use `@FilterField(json = true)` on POJO fields stored as JSON/JSONB. The generic `Map<String, Object>` is detected automatically.

## Operators

| Operator | Description | Example |
|----------|-------------|---------|
| `JSON_EXISTS` | key exists at root | `{"field":"preferences","operator":"JSON_EXISTS","value":"theme"}` |
| `JSON_PATH_EQ` | path value equals | `{"field":"preferences","operator":"JSON_PATH_EQ","values":["theme","dark"]}` |
| `JSON_CONTAINS` | JSONB contains object | `{"field":"metadata","operator":"JSON_CONTAINS","value":{"source":"web"}}` |
| `JSON_ARRAY_CONTAINS` | array contains value | `{"field":"metadata.tags","operator":"JSON_ARRAY_CONTAINS","value":"vip"}` |

On PostgreSQL, the JPA translator uses `jsonb_extract_path_text`, `@>`, `?`, etc.
