---
title: Interceptors
description: Add global or per-endpoint filter logic with interceptors.
weight: 30
---

## Global interceptor

Runs for every matching entity by default:

```java
@Component
@Order(1)
public class ActiveOnlyInterceptor implements FilterInterceptor<Product> {
    @Override
    public Class<Product> entityType() { return Product.class; }

    @Override
    public FilterResult<Product> preFilter(FilterContext<Product> ctx) {
        ctx.addFilter("active", Operator.EQ, true);
        return null;
    }
}
```

## Opt-in / endpoint-specific interceptor

Set `global()` to `false` and attach it to one or more endpoints:

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

Use the annotation to activate it:

```java
@FilterSearch(entity = Customer.class,
              interceptors = ExternalCustomerInterceptor.class)
@PostMapping("/external-search")
public FilterResult<Customer> externalSearch(...) { return null; }
```

You can also use `@EnableFilterEndpoint(interceptors = ...)` or override `AbstractFilterController#interceptors()`.
