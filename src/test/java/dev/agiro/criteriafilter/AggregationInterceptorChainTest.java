package dev.agiro.criteriafilter;

import dev.agiro.criteriafilter.interceptor.FilterContext;
import dev.agiro.criteriafilter.interceptor.FilterInterceptor;
import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.model.AggregationRequest;
import dev.agiro.criteriafilter.model.AggregationSpec;
import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.LogicalOperator;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.AggregationResult;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.FilterResult;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.repository.hibernatesearch.HibernateSearchCriteriaRepository;
import dev.agiro.criteriafilter.repository.opensearch.OpenSearchCriteriaRepository;
import dev.agiro.criteriafilter.sample.Product;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Interceptor semantics for aggregations, and the default
 * {@code CriteriaRepository#aggregate} of non-JPA backends.
 */
class AggregationInterceptorChainTest {

    private static final AggregationRequest REQUEST = new AggregationRequest(
            new FilterCondition("name", Operator.LIKE, "java", null),
            List.of("category"),
            List.of(AggregationSpec.count("n")));

    /** Records the request it receives and returns a fixed row. */
    static class RecordingRepository implements CriteriaRepository<Product> {
        AggregationRequest received;

        @Override
        public FilterResult<Product> filter(FilterRequest request, PageRequest page) {
            throw new AssertionError("filter must not be called");
        }

        @Override
        public AggregationResult aggregate(AggregationRequest request) {
            received = request;
            return new AggregationResult(List.of(Map.of("category", "BOOK", "n", 2L)));
        }

        @Override
        public Class<Product> entityType() {
            return Product.class;
        }
    }

    static class ActiveOnly implements FilterInterceptor<Product> {
        @Override
        public Class<Product> entityType() {
            return Product.class;
        }

        @Override
        public FilterResult<Product> preFilter(FilterContext<Product> context) {
            assertThat(context.isAggregation()).isTrue();
            assertThat(context.aggregation()).isEqualTo(REQUEST);
            assertThat(context.pageRequest()).isNotNull();
            context.addFilter("active", Operator.EQ, true);
            return null;
        }
    }

    @Test
    void filtersAddedInPreFilterAreAndedWithTheAggregationFilter() {
        var repository = new RecordingRepository();
        new FilterInterceptorChain(List.of(new ActiveOnly()))
                .executeAggregation(Product.class, REQUEST, repository, List.of());

        assertThat(repository.received.groupBy()).isEqualTo(REQUEST.groupBy());
        assertThat(repository.received.aggregations()).isEqualTo(REQUEST.aggregations());
        assertThat(repository.received.filter()).isEqualTo(new FilterGroup(LogicalOperator.AND, List.of(
                REQUEST.filter(), new FilterCondition("active", Operator.EQ, true, null))));
    }

    @Test
    void optInInterceptorsOnlyRunWhenRequested() {
        class OptIn extends ActiveOnly {
            @Override
            public boolean global() {
                return false;
            }
        }
        var chain = new FilterInterceptorChain(List.of(new OptIn()));

        var withoutIt = new RecordingRepository();
        chain.executeAggregation(Product.class, REQUEST, withoutIt, List.of());
        assertThat(withoutIt.received.filter()).isEqualTo(REQUEST.filter());

        var withIt = new RecordingRepository();
        chain.executeAggregation(Product.class, REQUEST, withIt, List.of(OptIn.class));
        assertThat(withIt.received.filter()).isInstanceOf(FilterGroup.class);
    }

    @Test
    void preFilterShortCircuitFailsClosedWithEmptyResult() {
        FilterInterceptor<Product> deny = new FilterInterceptor<>() {
            @Override
            public FilterResult<Product> preFilter(FilterContext<Product> context) {
                return new FilterResult<>(List.of(), 0, false);
            }
        };
        var repository = new RecordingRepository();
        AggregationResult result = new FilterInterceptorChain(List.of(deny))
                .executeAggregation(Product.class, REQUEST, repository, List.of());

        assertThat(result.rows()).isEmpty();
        assertThat(repository.received).isNull();
    }

    @Test
    void preAndPostAggregateHooksCanBeOverridden() {
        List<String> calls = new ArrayList<>();
        FilterInterceptor<Product> hooks = new FilterInterceptor<>() {
            @Override
            public FilterResult<Product> preFilter(FilterContext<Product> context) {
                calls.add("preFilter");
                return null;
            }

            @Override
            public AggregationResult preAggregate(FilterContext<Product> context) {
                calls.add("preAggregate");
                context.setAttribute("seen", true);
                return null;
            }

            @Override
            public AggregationResult postAggregate(FilterContext<Product> context, AggregationResult result) {
                calls.add("postAggregate:" + context.getAttribute("seen"));
                return new AggregationResult(List.of(Map.of("n", 42L)));
            }
        };
        AggregationResult result = new FilterInterceptorChain(List.of(hooks))
                .executeAggregation(Product.class, REQUEST, new RecordingRepository(), List.of());

        assertThat(calls).containsExactly("preAggregate", "postAggregate:true");
        assertThat(result.rows()).containsExactly(Map.of("n", 42L));
    }

    @Test
    void nonJpaBackendsThrowUnsupportedOperation() {
        assertThatThrownBy(() -> new OpenSearchCriteriaRepository<>(Product.class, null).aggregate(REQUEST))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> new HibernateSearchCriteriaRepository<>(Product.class, null).aggregate(REQUEST))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void resultRowsAreImmutableAndAllowNullValues() {
        var row = new java.util.HashMap<String, Object>();
        row.put("sum", null);
        AggregationResult result = new AggregationResult(List.of(row));
        assertThat(result.rows().get(0)).containsEntry("sum", null);
        assertThatThrownBy(() -> result.rows().get(0).put("x", 1)).isInstanceOf(UnsupportedOperationException.class);
        assertThat(new AggregationResult(null).rows()).isEmpty();
    }
}
