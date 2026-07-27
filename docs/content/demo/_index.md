---
title: Demo
description: Run the included demo application to explore all features.
weight: 10
---

The `criteria-filter-demo` project is included in the repository under the `criteria-filter-demo/` folder. It demonstrates:

- JPA + PostgreSQL filtering
- JSONB operators with `Customer`
- Global and opt-in interceptors
- `@EnableFilterEndpoint` and `@FilterSearch`

## Quick run

```bash
cd criteria-filter-demo
docker compose up -d
mvn spring-boot:run
```

Then open Swagger UI at `http://localhost:8080/swagger-ui.html` and try the endpoints.
