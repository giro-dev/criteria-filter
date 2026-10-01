package dev.agiro.criteriafilter.autoconfigure;

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
}
