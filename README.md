# criteria-filter

Internal library that translates a single JSON filter request into queries for
multiple backends. It replaces per-request reflection over entity fields with an
**annotation-driven metamodel resolved once at startup**, while keeping
extensibility for ambiguous cases (date formats, per-backend custom fields).

> **MVP status:** the **JPA** backend (translation to Spring Data
> `Specification`) is fully implemented. `OpenSearch` and `Hibernate Search`
> backends are stubs (`UnsupportedOperationException`) — the metamodel already
> resolves per-backend field names and date patterns for them.

## How it works

1. Annotate a filterable type with `@CriteriaFilter` and its fields with
   `@FilterField`. No attribute is mandatory — defaults are inferred from the
   Java type; explicit attributes override point by point.
2. At startup, `CriteriaFilterBeanInitializer` (a `ContextRefreshedEvent`
   listener) scans the configured packages, reflects over each entity **once**,
   and builds an immutable `EntityFilterMetadata`. A field pointing at a
   non-existent attribute **fails fast** at boot.
3. At request time, `FilterValidator` checks the `FilterRequest` against the
   metamodel (unknown field / unsupported operator / wrong arity) so a
   `400 Bad Request` is identical regardless of backend.
4. `CriteriaRepositoryRegistry` resolves the fixed backend repository per entity;
   client code never branches on the backend.

## Annotations

```java
@Entity
@CriteriaFilter(backend = Backend.JPA)
public class Product {
    @Id @FilterField Long id;
    @FilterField String name;                 // -> EQ, NE, LIKE, IN (inferred)
    @FilterField BigDecimal price;            // -> EQ, GT, LT, BETWEEN, ...
    @FilterField Instant createdAt;           // date pattern resolved per backend
    @FilterField(operators = {Operator.EQ}) boolean active; // explicit override
    String internalNote;                      // not filterable (no @FilterField)
}
```

### Operator inference by type

| Java type | Default operators |
|---|---|
| `String` | `EQ`, `NE`, `LIKE`, `IN`, `IS_NULL`, `IS_NOT_NULL` |
| Number | `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`, `IN`, `IS_NULL`, `IS_NOT_NULL` |
| Temporal | `EQ`, `NE`, `GT`, `GTE`, `LT`, `LTE`, `BETWEEN`, `IS_NULL`, `IS_NOT_NULL` |
| `Map` / `@FilterField(json = true)` | `EQ`, `NE`, `IS_NULL`, `IS_NOT_NULL`, `JSON_EXISTS`, `JSON_PATH_EQ`, `JSON_CONTAINS`, `JSON_ARRAY_CONTAINS` |

## Request schema

The `FilterRequest` schema is shared across every endpoint (document once as a
reusable OpenAPI component). A node is either a **condition** or a **group**;
Jackson deduces the subtype from the properties present.

```json
{
  "filter": {
    "combinator": "AND",
    "filters": [
      { "field": "name", "operator": "LIKE", "value": "java" },
      { "field": "price", "operator": "BETWEEN", "values": [10, 40] },
      { "field": "category", "operator": "IN", "values": ["BOOK", "TOY"] }
    ]
  }
}
```

## Controllers

```java
@RestController
@RequestMapping("/products")
public class ProductController extends AbstractFilterController<Product> {

    private final CriteriaRepositoryRegistry registry;
    private final FilterMetadataRegistry metadataRegistry;

    public ProductController(CriteriaRepositoryRegistry registry,
                             FilterMetadataRegistry metadataRegistry,
                             FilterValidator filterValidator,
                             FilterInterceptorChain interceptorChain) {
        super(filterValidator, interceptorChain);
        this.registry = registry;
        this.metadataRegistry = metadataRegistry;
    }

    @Override protected CriteriaRepository<Product> repository() { return registry.resolve(Product.class); }
    @Override protected Class<Product> entityType() { return Product.class; }
    @Override protected FilterMetadataRegistry metadataRegistry() { return metadataRegistry; }
}
```

`AbstractFilterController` exposes `POST /search` returning
`FilterResult<T>(content, totalHits, hasMore)`.

## Configuration

```yaml
criteria-filter:
  # Packages scanned for @CriteriaFilter. Empty = Spring Boot auto-config packages.
  base-packages:
    - com.example.domain
  # Default pattern for offset-less types (LocalDateTime / Timestamp).
  default-date-time-pattern: "yyyy-MM-dd'T'HH:mm:ss"
```

Auto-configuration registers all beans; just add the dependency.

## Build

```bash
mvn test        # 14 tests (metamodel, validation, JPA translation over H2, controller)
mvn package
```

Requires **Java 21**. Built on Spring Boot 3.

## Roadmap

See the open decisions in the design: OpenSearch / Hibernate Search backends,
relation/nested field mapping, dynamic per-request backend selection.
