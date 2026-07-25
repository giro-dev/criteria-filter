package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

/**
 * JPA implementation of {@link CriteriaRepository}. Builds a criteria query from
 * the {@link Specification} produced by {@link JpaSpecificationTranslator} and
 * runs it directly through the {@link EntityManager}, so no per-entity Spring
 * Data repository is required.
 */
public class JpaCriteriaRepository<T> implements CriteriaRepository<T> {

    private final EntityManager entityManager;
    private final Class<T> entityType;
    private final EntityFilterMetadata metadata;
    private final JpaSpecificationTranslator translator;

    public JpaCriteriaRepository(EntityManager entityManager, Class<T> entityType,
                                 EntityFilterMetadata metadata, JpaSpecificationTranslator translator) {
        this.entityManager = entityManager;
        this.entityType = entityType;
        this.metadata = metadata;
        this.translator = translator;
    }

    @Override
    public FilterResult<T> filter(FilterRequest request, PageRequest page) {
        Specification<T> specification = translator.toSpecification(request.filter(), metadata);
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<T> query = cb.createQuery(entityType);
        Root<T> root = query.from(entityType);
        Predicate predicate = specification.toPredicate(root, query, cb);
        if (predicate != null) {
            query.where(predicate);
        }
        query.select(root);

        // Fetch one extra row to decide hasMore without a second round-trip.
        List<T> rows = entityManager.createQuery(query)
                .setFirstResult(page.offset())
                .setMaxResults(page.size() + 1)
                .getResultList();

        boolean hasMore = rows.size() > page.size();
        List<T> content = hasMore ? rows.subList(0, page.size()) : rows;

        long totalHits = count(cb, specification);
        return new FilterResult<>(content, totalHits, hasMore);
    }

    private long count(CriteriaBuilder cb, Specification<T> specification) {
        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<T> root = countQuery.from(entityType);
        Predicate predicate = specification.toPredicate(root, countQuery, cb);
        if (predicate != null) {
            countQuery.where(predicate);
        }
        countQuery.select(cb.count(root));
        return entityManager.createQuery(countQuery).getSingleResult();
    }

    @Override
    public Class<T> entityType() {
        return entityType;
    }
}
