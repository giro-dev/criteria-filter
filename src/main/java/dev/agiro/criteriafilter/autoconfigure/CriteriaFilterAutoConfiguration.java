package dev.agiro.criteriafilter.autoconfigure;

import dev.agiro.criteriafilter.metamodel.CriteriaFilterBeanInitializer;
import dev.agiro.criteriafilter.metamodel.DateFieldResolver;
import dev.agiro.criteriafilter.metamodel.DatePatternResolver;
import dev.agiro.criteriafilter.metamodel.EntityFilterMetadataBuilder;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.jpa.JpaSpecificationTranslator;
import dev.agiro.criteriafilter.validation.FilterValidator;
import dev.agiro.criteriafilter.web.FilterExceptionHandler;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

import java.util.List;

/**
 * Auto-configuration for the criteria-filter library. Wires the metamodel
 * registry, per-entity repositories, validator and exception handler so the
 * library is plug-and-play in a Spring Boot application.
 */
@AutoConfiguration
@EnableConfigurationProperties(CriteriaFilterProperties.class)
public class CriteriaFilterAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JpaSpecificationTranslator jpaSpecificationTranslator() {
        return new JpaSpecificationTranslator();
    }

    @Bean
    @ConditionalOnMissingBean
    public DatePatternResolver datePatternResolver(ObjectProvider<DateFieldResolver> resolvers,
                                                   CriteriaFilterProperties properties) {
        return new DatePatternResolver(resolvers.orderedStream().toList(),
                properties.getDefaultDateTimePattern());
    }

    @Bean
    @ConditionalOnMissingBean
    public EntityFilterMetadataBuilder entityFilterMetadataBuilder(DatePatternResolver datePatternResolver) {
        return new EntityFilterMetadataBuilder(datePatternResolver);
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterMetadataRegistry filterMetadataRegistry() {
        return new FilterMetadataRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public CriteriaRepositoryRegistry criteriaRepositoryRegistry() {
        return new CriteriaRepositoryRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterValidator filterValidator(FilterMetadataRegistry registry) {
        return new FilterValidator(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public FilterExceptionHandler filterExceptionHandler() {
        return new FilterExceptionHandler();
    }

    @Bean
    public CriteriaFilterBeanInitializer criteriaFilterBeanInitializer(
            CriteriaFilterProperties properties,
            FilterMetadataRegistry metadataRegistry,
            CriteriaRepositoryRegistry repositoryRegistry,
            EntityFilterMetadataBuilder metadataBuilder,
            JpaSpecificationTranslator jpaTranslator,
            ObjectProvider<EntityManager> entityManagerProvider) {
        return new CriteriaFilterBeanInitializer(properties, metadataRegistry, repositoryRegistry,
                metadataBuilder, jpaTranslator, entityManagerProvider);
    }
}
