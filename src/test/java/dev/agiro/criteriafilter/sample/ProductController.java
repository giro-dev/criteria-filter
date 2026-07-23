package dev.agiro.criteriafilter.sample;

import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.web.AbstractFilterController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductController extends AbstractFilterController<Product, Long> {

    private final CriteriaRepositoryRegistry registry;

    public ProductController(CriteriaRepositoryRegistry registry) {
        this.registry = registry;
    }

    @Override
    protected CriteriaRepository<Product, ?> repository() {
        return registry.resolve(Product.class);
    }

    @Override
    protected Class<Product> entityType() {
        return Product.class;
    }
}
