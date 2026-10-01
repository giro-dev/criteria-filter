package dev.agiro.criteriafilter.autoconfigure;

import dev.agiro.criteriafilter.repository.jpa.JpaCriteriaRepository;
import dev.agiro.criteriafilter.validation.FilterValidator;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Configuration for the criteria-filter library.
 */
@ConfigurationProperties(prefix = "criteria-filter")
public class CriteriaFilterProperties {

    /**
     * Packages scanned for {@code @CriteriaFilter} types. When empty, the
     * Spring Boot auto-configuration packages of the host application are used.
     */
    private List<String> basePackages = List.of();

    /**
     * Default date/time pattern for {@code LocalDateTime}/{@code Timestamp}
     * (types with no inherent offset). Backends may override per field.
     */
    private String defaultDateTimePattern = "yyyy-MM-dd'T'HH:mm:ss";

    /** Maximum nesting depth of a filter tree. */
    private int maxDepth = FilterValidator.DEFAULT_MAX_DEPTH;

    /** Maximum number of conditions in a filter tree. */
    private int maxConditions = FilterValidator.DEFAULT_MAX_CONDITIONS;

    /** Maximum number of values in a single condition (e.g. {@code IN}). */
    private int maxValues = FilterValidator.DEFAULT_MAX_VALUES;

    /** Maximum number of {@code groupBy} fields in an aggregation request. */
    private int maxGroupBy = FilterValidator.DEFAULT_MAX_GROUP_BY;

    /** Maximum number of aggregate columns in an aggregation request. */
    private int maxAggregations = FilterValidator.DEFAULT_MAX_AGGREGATIONS;

    /** Maximum number of rows (groups) an aggregation may return. */
    private int maxAggregationGroups = JpaCriteriaRepository.DEFAULT_MAX_AGGREGATION_GROUPS;

    public List<String> getBasePackages() {
        return basePackages;
    }

    public void setBasePackages(List<String> basePackages) {
        this.basePackages = basePackages;
    }

    public String getDefaultDateTimePattern() {
        return defaultDateTimePattern;
    }

    public void setDefaultDateTimePattern(String defaultDateTimePattern) {
        this.defaultDateTimePattern = defaultDateTimePattern;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public int getMaxConditions() {
        return maxConditions;
    }

    public void setMaxConditions(int maxConditions) {
        this.maxConditions = maxConditions;
    }

    public int getMaxValues() {
        return maxValues;
    }

    public void setMaxValues(int maxValues) {
        this.maxValues = maxValues;
    }

    public int getMaxGroupBy() {
        return maxGroupBy;
    }

    public void setMaxGroupBy(int maxGroupBy) {
        this.maxGroupBy = maxGroupBy;
    }

    public int getMaxAggregations() {
        return maxAggregations;
    }

    public void setMaxAggregations(int maxAggregations) {
        this.maxAggregations = maxAggregations;
    }

    public int getMaxAggregationGroups() {
        return maxAggregationGroups;
    }

    public void setMaxAggregationGroups(int maxAggregationGroups) {
        this.maxAggregationGroups = maxAggregationGroups;
    }
}
