---
title: Internal vs External Search
description: Build two search endpoints for the same entity with different background filters.
weight: 10
---

## Goal

For `Customer`, expose:
- `POST /api/customers/search` for internal users (sees all customers).
- `POST /api/customers/external-search` for external users (active customers only).

## 1. Create the interceptor

```java
@Component
public class ExternalCustomerInterceptor implements FilterInterceptor<Customer> {
    @Override
    public Class<Customer> entityType() { return Customer.class; }

    @Override
    public boolean global() { return false; }

    @Override
    public FilterResult<Customer> preFilter(FilterContext<Customer> ctx) {
        ctx.addFilter("active", Operator.EQ, true);
        return null;
    }
}
```

## 2. Wire it to the external endpoint

```java
@RestController
@RequestMapping("/api/customers")
@EnableFilterEndpoint(entity = Customer.class)
public class CustomerController {

    @FilterSearch(entity = Customer.class,
                  interceptors = ExternalCustomerInterceptor.class)
    @PostMapping("/external-search")
    public FilterResult<Customer> externalSearch(...) { return null; }
}
```

## 3. Try it

Internal search (sees inactive):

```bash
curl -s http://localhost:8080/api/customers/search \
  -d '{"filter":{"field":"active","operator":"EQ","value":false}}'
```

External search (no inactive results):

```bash
curl -s http://localhost:8080/api/customers/external-search \
  -d '{"filter":{"field":"active","operator":"EQ","value":false}}'
```
