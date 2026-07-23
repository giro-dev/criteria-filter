package dev.agiro.criteriafilter.web;

import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.validation.FilterValidator;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Base controller exposing a uniform {@code POST /search} endpoint. Each
 * microservice extends this and implements {@link #repository()} and
 * {@link #entityType()}; the {@link FilterRequest} schema is shared across all
 * endpoints (document once as a reusable OpenAPI component).
 */
public abstract class AbstractFilterController<T, ID> {

    @Autowired
    private FilterValidator filterValidator;

    protected abstract CriteriaRepository<T, ?> repository();

    protected abstract Class<T> entityType();

    @PostMapping("/search")
    public ResponseEntity<FilterResult<T>> search(
            @RequestBody @Valid FilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        filterValidator.validate(request, entityType());
        return ResponseEntity.ok(repository().filter(request, new PageRequest(page, size)));
    }
}
