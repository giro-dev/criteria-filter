---
title: Aggregations
description: Grouped SUM/AVG/MIN/MAX/COUNT/COUNT_DISTINCT queries combined with the filter tree.
weight: 25
---

Every filter endpoint also exposes `POST /search/aggregate`. It takes the same
filter tree as `/search`, plus optional `groupBy` fields and a list of
aggregations, and returns one row per group.

```json
POST /products/search/aggregate
{
  "filter": { "and": [ { "field": "active", "operator": "EQ", "value": true } ] },
  "groupBy": ["category"],
  "aggregations": [
    { "field": "price", "function": "SUM", "alias": "totalPrice" },
    { "field": "price", "function": "AVG", "alias": "avgPrice" },
    { "function": "COUNT", "alias": "n" }
  ]
}
```

```json
{
  "rows": [
    { "category": "BOOK", "totalPrice": 80.00, "avgPrice": 40.0, "n": 2 },
    { "category": "FOOD", "totalPrice": 5.50,  "avgPrice": 5.5,  "n": 1 }
  ]
}
```

## Request

| Property | Required | Description |
|---|---|---|
| `filter` | no | Any `FilterNode`; omitted = match everything. Validated exactly like `/search`. |
| `groupBy` | no | Filter field names to group by. Omitted/empty = a single totals row. |
| `aggregations` | yes | At least one `{ field, function, alias }`. |

`alias` is optional; it defaults to `<function>_<field>` in lower case
(`sum_price`, `count_distinct_category`) or `count` for a field-less `COUNT`.
Aliases must be unique and must not clash with a `groupBy` field.

## Functions

| Function | Field | Allowed field types | Result |
|---|---|---|---|
| `SUM` | required | numbers | sum, `null` for no non-null values |
| `AVG` | required | numbers | average (double) |
| `MIN` / `MAX` | required | any `Comparable` (numbers, strings, dates, enums) | smallest / largest value |
| `COUNT` | optional | any | rows without `field`; non-null values with `field` |
| `COUNT_DISTINCT` | required | any | distinct non-null values |

Association fields (`@ManyToOne`) are grouped and counted by the target entity's
identifier, as in filter conditions.

## Results

- Each row holds the `groupBy` values followed by each alias, in request order.
- Rows are sorted by the `groupBy` fields (ascending).
- Without `groupBy` there is always exactly one row (`COUNT` is `0` when nothing matches).
- A grouped query producing more than `criteria-filter.max-aggregation-groups`
  groups (default 10 000) fails with `400 INVALID_FILTER`.

## Errors

Validation runs before the query and reuses the `/search` error shape:

| Problem | Status | `error` |
|---|---|---|
| Unknown or non-filterable `groupBy` / aggregation field | 400 | `UNKNOWN_FIELD` |
| `SUM`/`AVG` on a non-numeric field, `MIN`/`MAX` on a non-comparable field | 400 | `UNSUPPORTED_AGGREGATION` |
| Invalid filter tree | 400 | same as `/search` |
| Missing aggregations/function/field, duplicate alias, limits exceeded | 400 | `INVALID_FILTER` |
| Backend without aggregation support | 501 | `BACKEND_NOT_IMPLEMENTED` |

```json
{ "error": "UNSUPPORTED_AGGREGATION",
  "message": "Aggregate function 'AVG' is not supported for field 'name': requires a numeric field, but the field is String",
  "field": "name" }
```

## Interceptors

Aggregations run through the same `FilterInterceptorChain` as searches, so
global and opt-in interceptors apply to both. The default
`FilterInterceptor.preAggregate` delegates to `preFilter`: filters added with
`context.addFilter(...)` are ANDed with the aggregation filter, and a `preFilter`
that short-circuits yields an empty result. `context.isAggregation()` tells the
two apart; override `preAggregate` / `postAggregate` for aggregation-specific logic.

## Backends

Only the JPA backend implements `CriteriaRepository.aggregate` (a Criteria API
tuple query with `GROUP BY`). Other backends inherit the default, which throws
`UnsupportedOperationException`.

## Configuration

```yaml
criteria-filter:
  max-group-by: 16                # groupBy fields per request
  max-aggregations: 64            # aggregations per request
  max-aggregation-groups: 10000   # result rows of a grouped query
```

For `@EnableFilterEndpoint`, the path is `aggregatePath` (default
`search/aggregate`); set `includeAggregate = false` to not register it.
