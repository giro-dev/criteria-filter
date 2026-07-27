package dev.agiro.demo.controller;

import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.validation.FilterValidator;
import dev.agiro.criteriafilter.web.AbstractFilterController;
import dev.agiro.demo.entity.Product;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Product controller using AbstractFilterController (inheritance approach).
 * Demonstrates the classic way to expose filter endpoints.
 */
@RestController
@RequestMapping("/api/products")
@Tag(name = "Products", description = "Product search with criteria-filter (AbstractFilterController)")
public class ProductController extends AbstractFilterController<Product> {

    private final CriteriaRepositoryRegistry repositoryRegistry;
    private final FilterMetadataRegistry metadataRegistry;

    public ProductController(CriteriaRepositoryRegistry repositoryRegistry,
                             FilterMetadataRegistry metadataRegistry,
                             FilterValidator filterValidator,
                             FilterInterceptorChain interceptorChain) {
        super(filterValidator, interceptorChain);
        this.repositoryRegistry = repositoryRegistry;
        this.metadataRegistry = metadataRegistry;
    }

    @Override
    protected CriteriaRepository<Product> repository() {
        return repositoryRegistry.resolve(Product.class);
    }

    @Override
    protected Class<Product> entityType() {
        return Product.class;
    }

    @Override
    protected FilterMetadataRegistry metadataRegistry() {
        return metadataRegistry;
    }
}
