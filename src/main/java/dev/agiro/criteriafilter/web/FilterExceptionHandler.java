package dev.agiro.criteriafilter.web;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.exception.UnknownFieldException;
import dev.agiro.criteriafilter.exception.UnsupportedOperatorException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps the filter exception hierarchy to consistent HTTP status codes, so the
 * response is identical regardless of which backend raised it.
 */
@RestControllerAdvice
public class FilterExceptionHandler {

    @ExceptionHandler(UnknownFieldException.class)
    public ResponseEntity<FilterErrorResponse> onUnknownField(UnknownFieldException ex) {
        return ResponseEntity.badRequest()
                .body(FilterErrorResponse.of("UNKNOWN_FIELD", ex.getMessage(), ex.field()));
    }

    @ExceptionHandler(UnsupportedOperatorException.class)
    public ResponseEntity<FilterErrorResponse> onUnsupportedOperator(UnsupportedOperatorException ex) {
        return ResponseEntity.badRequest()
                .body(FilterErrorResponse.of("UNSUPPORTED_OPERATOR", ex.getMessage(), ex.field()));
    }

    @ExceptionHandler(FilterTranslationException.class)
    public ResponseEntity<FilterErrorResponse> onTranslation(FilterTranslationException ex) {
        return ResponseEntity.badRequest()
                .body(FilterErrorResponse.of("INVALID_FILTER", ex.getMessage(), null));
    }

    @ExceptionHandler(UnsupportedOperationException.class)
    public ResponseEntity<FilterErrorResponse> onUnsupportedBackend(UnsupportedOperationException ex) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(FilterErrorResponse.of("BACKEND_NOT_IMPLEMENTED", ex.getMessage(), null));
    }
}
