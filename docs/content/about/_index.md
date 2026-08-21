---
title: About
description: The criteria-filter project, its author, and its license.
weight: 30
---

criteria-filter is a Java library that turns Spring Boot entities into fully searchable REST endpoints without writing repository boilerplate.

## The project

A single JSON filter request is translated into a query for the target backend. Instead of reflecting over entity fields on every request, criteria-filter builds an **annotation-driven metamodel once at startup**, so unknown fields or unsupported operators fail fast and consistently.

- **JPA** (Spring Data `Specification`) is fully implemented, including PostgreSQL JSONB operators.
- **OpenSearch** and **Hibernate Search** backends are planned; the metamodel already resolves per-backend field names and date patterns.
- Interceptors let you add tenant isolation, soft-delete or security filters globally or per endpoint.

Current version: {{< param version >}}.

## About the author

criteria-filter is written and maintained by **Albert Giró Quer**, a backend
software engineer working mainly with Java and Spring Boot on the JVM, with an
interest in developer tooling and in APIs that stay simple as they grow.

The library grew out of a recurring, practical problem: every service ends up
reimplementing the same ad-hoc search endpoints, with the same reflection,
the same boilerplate `Specification` builders and the same inconsistent error
responses. criteria-filter is an attempt to solve that once, declaratively.

- [LinkedIn](https://es.linkedin.com/in/albert-giro-quer)
- [GitHub](https://github.com/giro-dev)

## Contributing

Questions, bug reports and pull requests are welcome in the
[GitHub repository](https://github.com/giro-dev/criteria-filter) —
open an [issue](https://github.com/giro-dev/criteria-filter/issues) to discuss
a change before sending a large one.

## License

Released under the
[MIT License](https://github.com/giro-dev/criteria-filter/blob/main/LICENSE).
