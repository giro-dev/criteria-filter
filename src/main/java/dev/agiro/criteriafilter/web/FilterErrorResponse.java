package dev.agiro.criteriafilter.web;

/**
 * Uniform error body for rejected filter requests.
 */
public record FilterErrorResponse(String error, String message, String field) {

    public static FilterErrorResponse of(String error, String message, String field) {
        return new FilterErrorResponse(error, message, field);
    }
}
