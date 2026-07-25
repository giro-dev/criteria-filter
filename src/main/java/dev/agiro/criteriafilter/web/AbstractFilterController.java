package dev.agiro.criteriafilter.web;

import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.validation.FilterValidator;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Base controller exposing uniform search endpoints:
 * <ul>
 *   <li>{@code POST /search} - Execute a filter query</li>
 *   <li>{@code GET /search/schema} - Get available filter options (fields, operators)</li>
 * </ul>
 *
 * <p>Each microservice extends this and implements {@link #repository()},
 * {@link #entityType()}, and {@link #metadataRegistry()}.
 */
@Tag(name = "Filter Search", description = "Dynamic filtering with nested AND/OR conditions")
public abstract class AbstractFilterController<T> {

    private final FilterValidator filterValidator;
    private final FilterInterceptorChain interceptorChain;

    protected AbstractFilterController(FilterValidator filterValidator,
                                        FilterInterceptorChain interceptorChain) {
        this.filterValidator = filterValidator;
        this.interceptorChain = interceptorChain;
    }

    protected abstract CriteriaRepository<T> repository();

    protected abstract Class<T> entityType();

    protected abstract FilterMetadataRegistry metadataRegistry();

    @Operation(
            summary = "Search with filters",
            description = """
                    Execute a search query with dynamic filters supporting nested AND/OR conditions.
                    
                    ## Filter Structure
                    
                    The filter can be either a single condition or a group of conditions:
                    
                    ### Single Condition
                    ```json
                    {"field": "name", "operator": "EQ", "value": "example"}
                    ```
                    
                    ### AND Group
                    ```json
                    {"and": [
                      {"field": "status", "operator": "EQ", "value": "ACTIVE"},
                      {"field": "price", "operator": "LT", "value": 100}
                    ]}
                    ```
                    
                    ### OR Group
                    ```json
                    {"or": [
                      {"field": "category", "operator": "EQ", "value": "BOOK"},
                      {"field": "category", "operator": "EQ", "value": "TOY"}
                    ]}
                    ```
                    
                    ### Nested Groups
                    ```json
                    {"and": [
                      {"field": "active", "operator": "EQ", "value": true},
                      {"or": [
                        {"field": "category", "operator": "EQ", "value": "BOOK"},
                        {"field": "price", "operator": "LT", "value": 20}
                      ]}
                    ]}
                    ```
                    
                    ## Available Operators
                    
                    | Operator | Arity | Description | Example |
                    |----------|-------|-------------|---------|
                    | `EQ` | single | Equals | `{"field": "status", "operator": "EQ", "value": "ACTIVE"}` |
                    | `NE` | single | Not equals | `{"field": "status", "operator": "NE", "value": "DELETED"}` |
                    | `GT` | single | Greater than | `{"field": "price", "operator": "GT", "value": 100}` |
                    | `GTE` | single | Greater or equal | `{"field": "price", "operator": "GTE", "value": 100}` |
                    | `LT` | single | Less than | `{"field": "price", "operator": "LT", "value": 50}` |
                    | `LTE` | single | Less or equal | `{"field": "price", "operator": "LTE", "value": 50}` |
                    | `LIKE` | single | Contains (case-insensitive) | `{"field": "name", "operator": "LIKE", "value": "java"}` |
                    | `IN` | multi | Value in list | `{"field": "category", "operator": "IN", "values": ["A", "B"]}` |
                    | `BETWEEN` | pair | Between two values | `{"field": "price", "operator": "BETWEEN", "values": [10, 100]}` |
                    | `IS_NULL` | none | Is null | `{"field": "deletedAt", "operator": "IS_NULL"}` |
                    | `IS_NOT_NULL` | none | Is not null | `{"field": "email", "operator": "IS_NOT_NULL"}` |
                    
                    ## JSONB Operators (PostgreSQL)
                    
                    | Operator | Description |
                    |----------|-------------|
                    | `JSON_CONTAINS` | JSONB contains |
                    | `JSON_EXISTS` | Key exists |
                    | `JSON_PATH_EQ` | Path value equals |
                    | `JSON_ARRAY_CONTAINS` | Array contains value |
                    
                    Use `GET /search/schema` to see available fields for this entity.
                    """
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
            description = "Filter request with nested AND/OR conditions",
            required = true,
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = FilterRequest.class),
                    examples = {
                            @ExampleObject(
                                    name = "Simple equality",
                                    summary = "Filter by single field",
                                    value = """
                                            {
                                              "filter": {
                                                "field": "status",
                                                "operator": "EQ",
                                                "value": "ACTIVE"
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "AND conditions",
                                    summary = "Multiple conditions with AND",
                                    value = """
                                            {
                                              "filter": {
                                                "and": [
                                                  {"field": "status", "operator": "EQ", "value": "ACTIVE"},
                                                  {"field": "price", "operator": "LT", "value": 100}
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "OR conditions",
                                    summary = "Multiple conditions with OR",
                                    value = """
                                            {
                                              "filter": {
                                                "or": [
                                                  {"field": "category", "operator": "EQ", "value": "BOOK"},
                                                  {"field": "category", "operator": "EQ", "value": "TOY"}
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "Nested AND/OR",
                                    summary = "Complex nested conditions",
                                    value = """
                                            {
                                              "filter": {
                                                "and": [
                                                  {"field": "active", "operator": "EQ", "value": true},
                                                  {
                                                    "or": [
                                                      {"field": "category", "operator": "EQ", "value": "BOOK"},
                                                      {"field": "price", "operator": "LT", "value": 20}
                                                    ]
                                                  }
                                                ]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "BETWEEN operator",
                                    summary = "Range filter with BETWEEN",
                                    value = """
                                            {
                                              "filter": {
                                                "field": "price",
                                                "operator": "BETWEEN",
                                                "values": [10, 100]
                                              }
                                            }
                                            """
                            ),
                            @ExampleObject(
                                    name = "IN operator",
                                    summary = "Filter by list of values",
                                    value = """
                                            {
                                              "filter": {
                                                "field": "category",
                                                "operator": "IN",
                                                "values": ["BOOK", "TOY", "FOOD"]
                                              }
                                            }
                                            """
                            )
                    }
            )
    )
    @PostMapping("/search")
    public ResponseEntity<FilterResult<T>> search(
            @RequestBody @Valid FilterRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        filterValidator.validate(request, entityType());
        FilterResult<T> result = interceptorChain.execute(
                entityType(), request, new PageRequest(page, size), repository());
        return ResponseEntity.ok(result);
    }

    @Operation(
            summary = "Get filter schema",
            description = "Returns available fields, supported operators, and example filter requests"
    )
    @GetMapping("/search/schema")
    public ResponseEntity<FilterSchemaResponse> schema() {
        EntityFilterMetadata metadata = metadataRegistry().require(entityType());
        return ResponseEntity.ok(FilterSchemaResponse.from(metadata));
    }
}
