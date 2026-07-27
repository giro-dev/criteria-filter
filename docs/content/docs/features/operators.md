---
title: Operators
description: Available filter operators per type and their JSON payload shape.
weight: 10
---

## Common operators

| Operator | Arity | Example |
|----------|-------|---------|
| `EQ` | single | `{"field":"status","operator":"EQ","value":"ACTIVE"}` |
| `NE` | single | `{"field":"status","operator":"NE","value":"DELETED"}` |
| `GT` | single | `{"field":"price","operator":"GT","value":100}` |
| `GTE` | single | `{"field":"price","operator":"GTE","value":100}` |
| `LT` | single | `{"field":"price","operator":"LT","value":50}` |
| `LTE` | single | `{"field":"price","operator":"LTE","value":50}` |
| `BETWEEN` | pair | `{"field":"price","operator":"BETWEEN","values":[10,100]}` |
| `IN` | list | `{"field":"category","operator":"IN","values":["A","B"]}` |
| `LIKE` | single | `{"field":"name","operator":"LIKE","value":"java"}` |
| `IS_NULL` | none | `{"field":"deletedAt","operator":"IS_NULL"}` |
| `IS_NOT_NULL` | none | `{"field":"email","operator":"IS_NOT_NULL"}` |

## Logical groups

Combine conditions with `and` or `or`:

```json
{
  "filter": {
    "and": [
      {"field": "active", "operator": "EQ", "value": true},
      {
        "or": [
          {"field": "category", "operator": "EQ", "value": "BOOK"},
          {"field": "price", "operator": "LT", "value": 20}
        ]
      }
    ]
  }
}
```
