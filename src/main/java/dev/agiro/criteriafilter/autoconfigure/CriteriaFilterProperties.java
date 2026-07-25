package dev.agiro.criteriafilter.autoconfigure;

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
}
