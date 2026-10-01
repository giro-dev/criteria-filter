# criteria-filter

[![Release](https://img.shields.io/endpoint?url=https%3A%2F%2Fgiro-dev.github.io%2Fcriteria-filter%2Freport%2Fbadges%2Frelease.json)](https://github.com/giro-dev/criteria-filter/releases/latest)
[![CI](https://github.com/giro-dev/criteria-filter/actions/workflows/ci.yml/badge.svg)](https://github.com/giro-dev/criteria-filter/actions/workflows/ci.yml)
[![Tests](https://img.shields.io/endpoint?url=https%3A%2F%2Fgiro-dev.github.io%2Fcriteria-filter%2Freport%2Fbadges%2Ftests.json)](https://giro-dev.github.io/criteria-filter/report/)
[![Coverage](https://img.shields.io/endpoint?url=https%3A%2F%2Fgiro-dev.github.io%2Fcriteria-filter%2Freport%2Fbadges%2Fcoverage.json)](https://giro-dev.github.io/criteria-filter/report/)
[![Java](https://img.shields.io/badge/java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/spring%20boot-3.3-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/license-MIT-blue)](LICENSE)

Turn a JSON filter tree into validated Spring Data JPA queries with two
annotations. criteria-filter gives APIs nested AND/OR filters, type-aware
operators, pagination, generated schema endpoints, and Swagger integration —
without repository query boilerplate.

[Documentation](https://giro-dev.github.io/criteria-filter/) ·
[Demo](criteria-filter-demo/) ·
[Test report](https://giro-dev.github.io/criteria-filter/report/)

## Why criteria-filter?

| Feature | What you get |
|---|---|
| Zero-boilerplate endpoints | `@EnableFilterEndpoint` generates `POST …/search` and `GET …/search/schema` |
| Safe, type-aware validation | Unknown fields, wrong operators, and bad arity become uniform `400`s |
| Nested boolean filters | Arbitrary AND/OR trees with operators inferred from Java types |
| Startup metamodel | Fields resolved once at boot — no per-request reflection, fail-fast on typos |
| Pagination built in | `page`/`size` params, `FilterResult` with `content`, `totalHits`, `hasMore` |
| OpenAPI/Swagger discovery | Dynamic endpoints show up in `/v3/api-docs`, inheriting the controller's `@Tag` |
| Interceptors | Global cross-cutting filters plus opt-in per-endpoint interceptors |
| PostgreSQL JSONB | `jsonb` containment, key-existence, and path operators on `Map` fields |

## Quick start

### 1. Add the dependency

```xml
<dependency>
    <groupId>dev.agiro</groupId>
    <artifactId>criteria-filter</artifactId>
    <version>0.1.0</version>
</dependency>
```

Requires Java 21+ and Spring Boot 3.3.x. Auto-configuration registers everything.

### 2. Mark an entity as filterable

```java
@Entity
@CriteriaFilter
public class Product {
    @Id
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private boolean active;
}
```

With no `@FilterField`, all non-static fields are exposed. Once any field has
`@FilterField`, it becomes an allow-list and only annotated fields are exposed.

### 3. Publish the endpoint

```java
@RestController
@RequestMapping("/api/products")
@EnableFilterEndpoint(entity = Product.class)
@Tag(name = "Products")
public class ProductController {
}
```

| Endpoint | Purpose |
|---|---|
| `POST /api/products/search` | Paginated filtered search |
| `GET /api/products/search/schema` | Fields and operators clients may use |

When springdoc is installed, the generated operations appear under **Products**
in Swagger UI.

## Try it

```bash
curl -X POST 'http://localhost:8080/api/products/search?page=0&size=20' \
  -H 'Content-Type: application/json' \
  -d '{
    "filter": {
      "and": [
        { "field": "active", "operator": "EQ", "value": true },
        { "field": "price", "operator": "BETWEEN", "values": [10, 100] }
      ]
    }
  }'
```

```json
{
  "content": [ { "id": 7, "name": "Java Mug", "price": 24.90 } ],
  "totalHits": 1,
  "hasMore": false
}
```

Return everything — an empty AND matches all (an empty OR matches none):

```json
{ "filter": { "and": [] } }
```

## Filter language

A filter node is either a **condition** (`field`, `operator`, `value`/`values`)
or a **group** combining children with `and`, `or`, or `combinator`/`filters`.

```json
{
  "filter": {
    "and": [
      { "field": "category", "operator": "IN", "values": ["BOOK", "TOY"] },
      {
        "or": [
          { "field": "name", "operator": "LIKE", "value": "java" },
          { "field": "price", "operator": "LT", "value": 15 }
        ]
      }
    ]
  }
}
```

| Category | Operators |
|---|---|
| Standard comparison | `EQ` `NE` `GT` `GTE` `LT` `LTE` |
| Text / set / range | `LIKE` `IN` `BETWEEN` |
| Nullability | `IS_NULL` `IS_NOT_NULL` |
| PostgreSQL JSONB | `JSON_CONTAINS` `JSON_CONTAINED_BY` `JSON_EXISTS` `JSON_EXISTS_ANY` `JSON_EXISTS_ALL` `JSON_PATH_EQ` `JSON_PATH_LIKE` `JSON_ARRAY_CONTAINS` `JSON_ARRAY_CONTAINS_ALL` `JSON_ARRAY_CONTAINS_ANY` |

`LIKE` is a case-insensitive *contains*; `%`, `_`, and `\` in the value match
literally.

## Control the filter surface

```java
@Entity
@CriteriaFilter
public class Product {
    @FilterField(name = "q")                 // exposed as "q" instead of "name"
    private String name;

    @FilterField(operators = {Operator.EQ, Operator.IN})
    private String category;                 // restrict allowed operators

    @FilterField(excluded = true)
    private String internalNote;             // never filterable
}
```

- With no `@FilterField` attributes, operators are inferred from the field's
  Java type (e.g. strings get `EQ`, `NE`, `LIKE`, `IN`, null checks).
- Adding `@FilterField` to any field turns exposure into an allow-list;
  `excluded = true` hides a field even inside the allow-list.

## More ways to expose a search

| Approach | When to use |
|---|---|
| `@EnableFilterEndpoint` | Zero-boilerplate recommended default — generates both endpoints |
| `@FilterSearch` / `@FilterSchema` | Custom paths and method-level control inside your own controller |
| `AbstractFilterController<T>` | Inheritance-based customization of search behavior |

See the [demo](criteria-filter-demo/) and
[docs](https://giro-dev.github.io/criteria-filter/) for full examples.

## Interceptors

Global interceptors run on every search for cross-cutting concerns like tenant
isolation or soft delete. Endpoint interceptors are opt-in per endpoint — e.g.
an external, active-only view of customers:

```java
@FilterSearch(entity = Customer.class, interceptors = ExternalCustomerInterceptor.class)
```

Details: [interceptors](docs/content/docs/features/interceptors.md).

## Configuration

```yaml
criteria-filter:
  # Optional: defaults to the Spring Boot auto-configuration packages.
  base-packages:
    - com.example.domain
  default-date-time-pattern: "yyyy-MM-dd'T'HH:mm:ss"
  max-depth: 32        # group nesting depth
  max-conditions: 1000 # conditions in the whole tree
  max-values: 1000     # values in a single condition (IN, JSON_EXISTS_ANY, ...)
```

## Backend status

| Backend | Status |
|---|---|
| Spring Data JPA | Ready — translates to `Specification` |
| PostgreSQL JSONB | Ready — JPA native predicates on `Map`/`jsonb` fields |
| OpenSearch | Planned — metamodel prepared, repository throws `UnsupportedOperationException` |
| Hibernate Search | Planned — metamodel prepared, repository throws `UnsupportedOperationException` |

## Demo and development

```bash
mvn test                                   # run the suite
cd criteria-filter-demo && mvn spring-boot:run   # start the demo app
```

- [Demo README](criteria-filter-demo/)
- [Published documentation](https://giro-dev.github.io/criteria-filter/)

Licensed under [MIT](LICENSE).
