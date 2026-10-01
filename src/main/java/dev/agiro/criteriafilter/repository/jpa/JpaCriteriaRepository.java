package dev.agiro.criteriafilter.repository.jpa;

import dev.agiro.criteriafilter.exception.FilterTranslationException;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadata;
import dev.agiro.criteriafilter.model.AggregationRequest;
import dev.agiro.criteriafilter.model.AggregationSpec;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.AggregationResult;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Tuple;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * JPA implementation of {@link CriteriaRepository}. Builds a criteria query from
 * the {@link Specification} produced by {@link JpaSpecificationTranslator} and
 * runs it directly through the {@link EntityManager}, so no per-entity Spring
 * Data repository is required.
 */
public class JpaCriteriaRepository<T> implements CriteriaRepository<T> {

    /** Default upper bound on the number of groups an aggregation may return. */
    public static final int DEFAULT_MAX_AGGREGATION_GROUPS = 10_000;

    private final EntityManager entityManager;
    private final Class<T> entityType;
    private final EntityFilterMetadata metadata;
    private final JpaSpecificationTranslator translator;
    private final int maxAggregationGroups;

    public JpaCriteriaRepository(EntityManager entityManager, Class<T> entityType,
                                 EntityFilterMetadata metadata, JpaSpecificationTranslator translator) {
        this(entityManager, entityType, metadata, translator, DEFAULT_MAX_AGGREGATION_GROUPS);
    }

    /**
     * @param maxAggregationGroups maximum number of rows (groups) an aggregation may
     *                             return; larger results are rejected with HTTP 400
     */
    public JpaCriteriaRepository(EntityManager entityManager, Class<T> entityType,
                                 EntityFilterMetadata metadata, JpaSpecificationTranslator translator,
                                 int maxAggregationGroups) {
        this.entityManager = entityManager;
        this.entityType = entityType;
        this.metadata = metadata;
        this.translator = translator;
        this.maxAggregationGroups = maxAggregationGroups;
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
    public AggregationResult aggregate(AggregationRequest request) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Tuple> query = cb.createTupleQuery();
        Root<T> root = query.from(entityType);
        query.where(translator.toPredicate(request.filter(), metadata, root, cb));

        List<Expression<?>> groupKeys = new ArrayList<>(request.groupBy().size());
        List<Selection<?>> selections = new ArrayList<>();
        for (String field : request.groupBy()) {
            Expression<?> key = translator.resolveValuePath(metadata.require(field), root);
            groupKeys.add(key);
            selections.add(key.alias(field));
        }
        for (AggregationSpec spec : request.aggregations()) {
            selections.add(aggregateExpression(spec, root, cb).alias(spec.resolvedAlias()));
        }
        query.multiselect(selections);
        if (!groupKeys.isEmpty()) {
            query.groupBy(groupKeys);
            query.orderBy(groupKeys.stream().map(cb::asc).toList());
        }

        List<Tuple> tuples = entityManager.createQuery(query)
                .setMaxResults(maxAggregationGroups + 1)
                .getResultList();
        if (tuples.size() > maxAggregationGroups) {
            throw new FilterTranslationException("Aggregation exceeds the maximum of "
                    + maxAggregationGroups + " groups; narrow the filter or the groupBy fields");
        }

        List<Map<String, Object>> rows = new ArrayList<>(tuples.size());
        for (Tuple tuple : tuples) {
            Map<String, Object> row = new LinkedHashMap<>();
            for (Selection<?> selection : selections) {
                row.put(selection.getAlias(), tuple.get(selection.getAlias()));
            }
            rows.add(row);
        }
        return new AggregationResult(rows);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Expression<?> aggregateExpression(AggregationSpec spec, Root<T> root, CriteriaBuilder cb) {
        if (spec.field() == null || spec.field().isBlank()) {
            return switch (spec.function()) {
                case COUNT -> cb.count(root);
                default -> throw new FilterTranslationException(
                        "Aggregate function '" + spec.function() + "' requires a field");
            };
        }
        Path<?> path = translator.resolveValuePath(metadata.require(spec.field()), root);
        return switch (spec.function()) {
            case SUM -> cb.sum((Expression<Number>) path);
            case AVG -> cb.avg((Expression<Number>) path);
            case MIN -> cb.least((Expression<Comparable>) path);
            case MAX -> cb.greatest((Expression<Comparable>) path);
            case COUNT -> cb.count(path);
            case COUNT_DISTINCT -> cb.countDistinct(path);
        };
    }

    @Override
    public Class<T> entityType() {
        return entityType;
    }
}
