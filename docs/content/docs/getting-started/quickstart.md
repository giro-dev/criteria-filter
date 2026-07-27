---
title: Quick Start
description: Build a search endpoint in three steps.
weight: 20
---

## 1. Annotate the entity

```java
@Entity
@CriteriaFilter
public class Product {
    @Id @GeneratedValue
    private Long id;
    private String name;
    private String category;
    private BigDecimal price;
    private boolean active;
}
```

## 2. Enable the search endpoint

```java
@RestController
@RequestMapping("/api/products")
@EnableFilterEndpoint(entity = Product.class)
public class ProductController {
}
```

## 3. Search

```bash
curl -X POST http://localhost:8080/api/products/search \
  -H "Content-Type: application/json" \
  -d '{
    "filter": {
      "and": [
        {"field": "active", "operator": "EQ", "value": true},
        {"field": "price", "operator": "LT", "value": 100}
      ]
    }
  }'
```

That's it — no repository methods, no Specification builders.
