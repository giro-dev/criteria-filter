package dev.agiro.criteriafilter.interceptor;

import dev.agiro.criteriafilter.model.FilterCondition;
import dev.agiro.criteriafilter.model.FilterGroup;
import dev.agiro.criteriafilter.model.FilterNode;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.model.LogicalOperator;
import dev.agiro.criteriafilter.model.Operator;
import dev.agiro.criteriafilter.repository.PageRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Mutable context passed to {@link FilterInterceptor} methods.
 * Allows interceptors to modify the filter request, add conditions,
 * and share attributes between interceptors.
 *
 * @param <T> Entity type
 */
public class FilterContext<T> {

    private final Class<T> entityType;
    private FilterRequest request;
    private PageRequest pageRequest;
    private final Map<String, Object> attributes = new HashMap<>();
    private final List<FilterNode> additionalFilters = new ArrayList<>();

    public FilterContext(Class<T> entityType, FilterRequest request, PageRequest pageRequest) {
        this.entityType = entityType;
        this.request = request;
        this.pageRequest = pageRequest;
    }

    public Class<T> entityType() {
        return entityType;
    }

    public FilterRequest request() {
        return request;
    }

    public void setRequest(FilterRequest request) {
        this.request = request;
    }

    public PageRequest pageRequest() {
        return pageRequest;
    }

    public void setPageRequest(PageRequest pageRequest) {
        this.pageRequest = pageRequest;
    }

    /**
     * Adds an additional filter condition that will be ANDed with the original filter.
     */
    public FilterContext<T> addFilter(String field, Operator operator, Object value) {
        additionalFilters.add(new FilterCondition(field, operator, value, null));
        return this;
    }

    /**
     * Adds an additional filter condition with multiple values.
     */
    public FilterContext<T> addFilter(String field, Operator operator, List<Object> values) {
        additionalFilters.add(new FilterCondition(field, operator, null, values));
        return this;
    }

    /**
     * Adds a custom filter node (condition or group).
     */
    public FilterContext<T> addFilter(FilterNode node) {
        additionalFilters.add(node);
        return this;
    }

    /**
     * Returns the list of additional filters added by interceptors.
     */
    public List<FilterNode> additionalFilters() {
        return additionalFilters;
    }

    /**
     * Builds the final filter request, merging original and additional filters.
     */
    public FilterRequest buildFinalRequest() {
        if (additionalFilters.isEmpty()) {
            return request;
        }

        FilterNode originalFilter = request.filter();
        List<FilterNode> allFilters = new ArrayList<>();

        if (originalFilter != null) {
            allFilters.add(originalFilter);
        }
        allFilters.addAll(additionalFilters);

        if (allFilters.isEmpty()) {
            return request;
        }

        if (allFilters.size() == 1) {
            return new FilterRequest(allFilters.get(0));
        }

        FilterGroup combined = new FilterGroup(LogicalOperator.AND, allFilters);
        return new FilterRequest(combined);
    }

    /**
     * Gets a custom attribute set by a previous interceptor.
     */
    @SuppressWarnings("unchecked")
    public <V> V getAttribute(String key) {
        return (V) attributes.get(key);
    }

    /**
     * Sets a custom attribute to share data between interceptors.
     */
    public void setAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Checks if an attribute exists.
     */
    public boolean hasAttribute(String key) {
        return attributes.containsKey(key);
    }
}
