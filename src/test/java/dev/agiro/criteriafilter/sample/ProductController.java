package dev.agiro.criteriafilter.sample;

import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.validation.FilterValidator;
import dev.agiro.criteriafilter.web.AbstractFilterController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
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
