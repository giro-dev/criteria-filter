package dev.agiro.criteriafilter.model;

import jakarta.validation.constraints.NotNull;

/**
 * Root of a filter query. Shared schema across every endpoint and microservice,
 * so it can be documented once as a reusable OpenAPI component.
 */
public record FilterRequest(
        @NotNull FilterNode filter
) {
}
