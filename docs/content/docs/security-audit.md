---
title: Security Audit
description: Security, edge-case and data-type audit of the filter library and demo, with the bugs found and fixed.
weight: 40
---

The automated suite covering these findings has 200 tests: unit, MockMvc, H2 integration, 11 PostgreSQL 16
Testcontainers JSONB tests (skipped when Docker is unavailable) and `FilterPerformanceBudgetTest`, which checks
that the largest allowed filters respond within 3 s and oversized ones are rejected within 2 s.
CI runs it on every push and pull request (`.github/workflows/ci.yml`).

## 1. Structure and public API

| Area | Location | Notes |
|---|---|---|
| Library | `src/main/java/dev/agiro/criteriafilter` | Spring Boot auto-config, JPA backend |
| Demo | `criteria-filter-demo` | PostgreSQL, products/brands/customers/orders |
| Tests | `src/test/java` (JUnit 5, Spring Boot Test, H2, MockMvc) | |

- Annotations: `@CriteriaFilter` (entity), `@FilterField` (name, operators, json, ignore), `@FilterEndpoint` (controller methods; `executeDefault`), interceptors.
- Request model: `FilterRequest{filter, page, size}`; `FilterNode` = `FilterCondition{field, operator, value | values}` or `FilterGroup{and | or | combinator+filters}`. Jackson deduction picks the subtype.
- Standard operators: `EQ NE GT GTE LT LTE LIKE(contains, case-insensitive) IN BETWEEN IS_NULL IS_NOT_NULL`, inferred per Java type.
- PostgreSQL JSONB operators (`PostgresJsonbOperatorHandler`): `JSON_CONTAINS JSON_CONTAINED_BY JSON_EXISTS JSON_EXISTS_ANY JSON_EXISTS_ALL JSON_PATH_EQ JSON_PATH_LIKE JSON_ARRAY_CONTAINS(1 or 2 operands) JSON_ARRAY_CONTAINS_ALL JSON_ARRAY_CONTAINS_ANY`.
- Coercion (`ValueCoercion`): String, enums, Boolean, UUID, Integer/Long/Short/Byte, Double/Float, BigDecimal, BigInteger, Instant, LocalDate, LocalDateTime (configurable pattern), OffsetDateTime, ZonedDateTime.
- Pagination: `PageResult{content, page, size, total, hasMore}`; schema endpoint `<path>/schema` describes fields and allowed operators.
- Errors: `FilterExceptionHandler` -> HTTP 400 `INVALID_FILTER` / `UNKNOWN_FIELD` / `UNSUPPORTED_OPERATOR` / (new) `INVALID_PAGE`.
- New config: `criteria-filter.max-depth` (32), `max-conditions` (1000), `max-values` (1000).

## 2. Security results

| Attack | Result |
|---|---|
| SQL injection via field names / dotted paths (`name; DROP TABLE`, `brand.name--`) | Not exploitable: fields whitelisted from metadata -> 400 `UNKNOWN_FIELD` |
| Operator smuggling (`"operator":"EQ OR 1=1"`, lowercase, SQL text) | Not exploitable: enum deserialization -> 400 |
| SQL injection via values (EQ/LIKE/IN, `' OR '1'='1`) | Not exploitable: bound parameters; verified 0 rows returned |
| JSONB injection via keys/paths/values | Not exploitable after rewrite (bound literals, fixed SQL operators); malformed JSON -> 400 |
| Prototype pollution (`__proto__`, `constructor`) | N/A for Java; strict mapper rejects, Spring mapper ignores. No effect on results |
| Deep nesting / wide trees / huge IN lists | **Was DoS** (bugs 5, 6) -> now 400 |
| Type confusion (objects/arrays/booleans in scalar fields) | **Was silently mis-filtered** (bugs 7, 8) -> now 400 |

## 3. Bugs found (all fixed, with regression tests)

Repro requests are `POST` with `Content-Type: application/json` against the demo on `localhost:8080`.

1. **Hibernate proxy serialization breaks product search (demo).**
   `POST /api/products/search {"filter":{"and":[]}}` -> HTTP 500 `No serializer found for class ...ByteBuddyInterceptor`. Expected 200.
   Fix: `@JsonIgnoreProperties({"hibernateLazyInitializer","handler"})` on demo `Brand`. Verified via demo (200, 4+ products).

2. **All JSONB operators fail on PostgreSQL.** Column was cast to `text`.
   `POST /api/customers/search {"filter":{"field":"preferences","operator":"JSON_CONTAINS","value":{"theme":"dark"}}}` -> 500 `operator does not exist: text @> jsonb` (similar for `jsonb_exists(text,...)`, `jsonb_extract_path_text(text,...)`). Expected matching customers.
   Fix: `PostgresJsonbOperatorHandler` rewritten (jsonb casts, Jackson serialization, path splitting on `.`, FAIL_ON_TRAILING_TOKENS). Tests: `PostgresJsonbIntegrationTest`.

3. **`JSON_ARRAY_CONTAINS` [path, value] form rejected** although documented in demo README.
   `{"field":"metadata","operator":"JSON_ARRAY_CONTAINS","values":["tags","vip"]}` -> 400 arity error. Fix: `Arity.SINGLE_OR_PAIR`. Test: `PostgresJsonbIntegrationTest`, `FilterValidatorHardeningTest`.

4. **Invalid pagination -> HTTP 500.** `{"filter":{"and":[]},"page":-1}`, `"size":0`, or `"page":2147483647,"size":100` (int overflow of offset) -> 500. Expected 400.
   Fix: `PageRequest` validation + `InvalidPageRequestException` -> 400 `INVALID_PAGE`. Tests: `PageRequestTest`, `FilterSecurityWebTest`.

5. **Deep nesting -> StackOverflowError.** ~450 nested `{"and":[...]}` groups -> 500 / thread stack exhaustion. Fix: `max-depth` (32) -> 400. Test: `FilterSecurityWebTest`, `FilterValidatorHardeningTest`.

6. **Unbounded condition/value counts.** 40k conditions took many seconds; 70k IN values -> 500 (PostgreSQL 65,535 bind-parameter limit). Fix: `max-conditions`/`max-values` (1000) -> 400.

7. **Lenient boolean coercion.** `{"field":"active","operator":"EQ","value":"yes"}` (or `1`) silently became `false` and returned inactive customers. Expected 400. Fix: only `true`/`false`. Test: `ValueCoercionTest`, `JpaOperatorCoverageIntegrationTest`.

8. **Structured values stringified into scalar filters.** `{"field":"name","operator":"EQ","value":{"a":1}}` compared against `"{a=1}"`. Expected 400. Fix in `ValueCoercion`.

9. **NaN/Infinity/huge numbers.** `"value":"NaN"` / `"Infinity"` on a Double field accepted; `1e999999999` -> 500 from DB numeric overflow. Expected 400. Fix: finite check + 1000-digit bound.

10. **LIKE wildcards not escaped.** `{"field":"name","operator":"LIKE","value":"%"}` matched every row; `_` matched any char. Expected literal contains. Fix: `LikePatterns` with explicit escape (also `JSON_PATH_LIKE`). Test: `LikePatternsTest`, `JpaOperatorCoverageIntegrationTest`.

11. **Empty OR group matched everything.** `{"filter":{"or":[]}}` returned all rows (translated to conjunction). Expected none (Boolean identity of OR). Fix in `JpaSpecificationTranslator`.

12. **Ambiguous group syntax silently picked a branch.** `{"filter":{"and":[A],"or":[B]}}` evaluated only `and`. Expected 400. Fix in `FilterGroup`. Test: `FilterRequestJsonTest`.

13. **Association equality crashes.** `POST /api/products/search {"filter":{"field":"brand","operator":"EQ","value":1}}` -> 500 `Cannot compare left expression of type Brand with right expression of type Integer`. Fix: compare by the target's id attribute. Verified via demo (4 Apple products).

14. **NUL characters -> HTTP 500** (`invalid byte sequence for encoding "UTF8": 0x00`). `{"field":"name","operator":"LIKE","value":"\u0000"}`. Fix: validator rejects with 400.

15. **Missing field/operator, null operands, both `value` and `values`** reached the translator and produced 500/NPE or ambiguous results. Fix: `FilterValidator` -> 400.

## 4. Edge cases verified (behaving correctly)

Unicode (accents, CJK, emoji, RTL) round-trips and matches; `Long.MAX_VALUE`/`MIN_VALUE` exact; integer overflow on Integer fields -> 400; dates: `LocalDate`, offset timestamps compared as instants across time zones, `Instant` with `Z`; contradictory conditions (`price > 10 AND price < 5`) -> 0 rows; duplicate conditions idempotent; duplicate JSON keys -> last wins (Jackson default, documented in `FilterRequestJsonTest`); empty `IN` -> 400; `BETWEEN` with reversed bounds -> 0 rows (SQL semantics).

## 5. Not fixed / notes

- Demo `POST /api/orders/pending` returns HTTP 200 with an empty body: it is a `@FilterEndpoint(executeDefault = false)` placeholder that returns `null`. Left as-is.
- Duplicate JSON keys are accepted (last wins); enable `JsonParser.Feature.STRICT_DUPLICATE_DETECTION` if this should be rejected.
- Testcontainers bumped 1.19.8 -> 1.21.4 (BOM) because 1.19.8 cannot talk to Docker Engine 29 (API >= 1.44); this only affects tests.
