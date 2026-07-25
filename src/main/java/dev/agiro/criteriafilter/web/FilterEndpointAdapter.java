package dev.agiro.criteriafilter.web;

import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.validation.FilterValidator;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;

/**
 * Stateless handler invoked for filter endpoints registered dynamically by
 * {@link FilterEndpointHandlerMapping}. Each instance is bound to one entity type.
 */
public class FilterEndpointAdapter {

    private final Class<?> entityType;
    private final FilterValidator filterValidator;
    private final CriteriaRepositoryRegistry repositoryRegistry;
    private final FilterMetadataRegistry metadataRegistry;
    private final FilterInterceptorChain interceptorChain;

    public FilterEndpointAdapter(Class<?> entityType,
                                  FilterValidator filterValidator,
                                  CriteriaRepositoryRegistry repositoryRegistry,
                                  FilterMetadataRegistry metadataRegistry,
                                  FilterInterceptorChain interceptorChain) {
        this.entityType = entityType;
        this.filterValidator = filterValidator;
        this.repositoryRegistry = repositoryRegistry;
        this.metadataRegistry = metadataRegistry;
        this.interceptorChain = interceptorChain;
    }

    @SuppressWarnings("unchecked")
    public ResponseEntity<FilterResult<Object>> search(
            @RequestBody @Valid FilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        filterValidator.validate(request, entityType);
        CriteriaRepository<Object> repository = (CriteriaRepository<Object>) repositoryRegistry.resolve(entityType);
        FilterResult<Object> result = interceptorChain.execute(
                (Class<Object>) entityType, request, new PageRequest(page, size), repository);
        return ResponseEntity.ok(result);
    }

    public ResponseEntity<FilterSchemaResponse> schema() {
        return ResponseEntity.ok(FilterSchemaResponse.from(metadataRegistry.require(entityType)));
    }

    /** Reflective lookup of the search method. */
    public Method searchMethod() {
        try {
            return getClass().getMethod("search", FilterRequest.class, int.class, int.class);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("FilterEndpointAdapter.search not found", e);
        }
    }

    /** Reflective lookup of the schema method. */
    public Method schemaMethod() {
        try {
            return getClass().getMethod("schema");
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("FilterEndpointAdapter.schema not found", e);
        }
    }
}
