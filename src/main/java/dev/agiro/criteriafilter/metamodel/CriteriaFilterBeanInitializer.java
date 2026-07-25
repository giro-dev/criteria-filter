package dev.agiro.criteriafilter.metamodel;

import dev.agiro.criteriafilter.annotation.CriteriaFilter;
import dev.agiro.criteriafilter.autoconfigure.CriteriaFilterProperties;
import dev.agiro.criteriafilter.model.Backend;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.hibernatesearch.HibernateSearchCriteriaRepository;
import dev.agiro.criteriafilter.repository.jpa.JpaCriteriaRepository;
import dev.agiro.criteriafilter.repository.jpa.JpaSpecificationTranslator;
import dev.agiro.criteriafilter.repository.opensearch.OpenSearchCriteriaRepository;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Builds the immutable filter metamodel and the per-entity repositories once, at
 * startup, on {@link ContextRefreshedEvent}. Reflection over entity fields
 * happens here exactly once; request time is reflection-free.
 *
 * <p>Fail-fast: a typo/refactor that leaves an annotated field pointing at a
 * non-existent attribute aborts context startup rather than being silenced.
 */
public class CriteriaFilterBeanInitializer implements ApplicationListener<ContextRefreshedEvent> {

    private static final Logger log = LoggerFactory.getLogger(CriteriaFilterBeanInitializer.class);

    private final CriteriaFilterProperties properties;
    private final FilterMetadataRegistry metadataRegistry;
    private final CriteriaRepositoryRegistry repositoryRegistry;
    private final EntityFilterMetadataBuilder metadataBuilder;
    private final JpaSpecificationTranslator jpaTranslator;
    private final ObjectProvider<EntityManager> entityManagerProvider;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    public CriteriaFilterBeanInitializer(CriteriaFilterProperties properties,
                                         FilterMetadataRegistry metadataRegistry,
                                         CriteriaRepositoryRegistry repositoryRegistry,
                                         EntityFilterMetadataBuilder metadataBuilder,
                                         JpaSpecificationTranslator jpaTranslator,
                                         ObjectProvider<EntityManager> entityManagerProvider) {
        this.properties = properties;
        this.metadataRegistry = metadataRegistry;
        this.repositoryRegistry = repositoryRegistry;
        this.metadataBuilder = metadataBuilder;
        this.jpaTranslator = jpaTranslator;
        this.entityManagerProvider = entityManagerProvider;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        List<String> basePackages = resolveBasePackages(event);
        Map<Class<?>, EntityFilterMetadata> metadataByEntity = new LinkedHashMap<>();
        Map<Class<?>, CriteriaRepository<?>> repositoriesByEntity = new LinkedHashMap<>();

        for (Class<?> annotatedType : scan(basePackages)) {
            EntityFilterMetadata metadata = metadataBuilder.build(annotatedType);
            EntityFilterMetadata clash = metadataByEntity.putIfAbsent(metadata.entityType(), metadata);
            if (clash != null) {
                throw new IllegalStateException("Two @CriteriaFilter types map to entity "
                        + metadata.entityType().getName());
            }
            repositoriesByEntity.put(metadata.entityType(), buildRepository(metadata));
            log.debug("Registered @CriteriaFilter metadata for {} (backend={}, fields={})",
                    metadata.entityType().getName(), metadata.backend(), metadata.fields().keySet());
        }

        metadataRegistry.initialize(metadataByEntity);
        repositoryRegistry.initialize(repositoriesByEntity);
        log.info("criteria-filter initialized: {} filterable entities", metadataByEntity.size());
    }

    private CriteriaRepository<?> buildRepository(EntityFilterMetadata metadata) {
        Class<?> entityType = metadata.entityType();
        Backend backend = metadata.backend();
        return switch (backend) {
            case JPA -> new JpaCriteriaRepository<>(requireEntityManager(entityType), entityType,
                    metadata, jpaTranslator);
            case OPENSEARCH -> new OpenSearchCriteriaRepository<>(entityType, metadata);
            case HIBERNATE_SEARCH -> new HibernateSearchCriteriaRepository<>(entityType, metadata);
        };
    }

    private EntityManager requireEntityManager(Class<?> entityType) {
        EntityManager em = entityManagerProvider.getIfAvailable();
        if (em == null) {
            throw new IllegalStateException("Entity " + entityType.getName()
                    + " declares backend JPA but no EntityManager is available");
        }
        return em;
    }

    private List<Class<?>> scan(List<String> basePackages) {
        var provider = new org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new AnnotationTypeFilter(CriteriaFilter.class));
        List<Class<?>> types = new java.util.ArrayList<>();
        for (String basePackage : basePackages) {
            for (var candidate : provider.findCandidateComponents(basePackage)) {
                String className = candidate.getBeanClassName();
                if (className == null) {
                    continue;
                }
                try {
                    types.add(Class.forName(className));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException("Cannot load @CriteriaFilter type " + className, e);
                }
            }
        }
        return types;
    }

    private List<String> resolveBasePackages(ContextRefreshedEvent event) {
        if (!properties.getBasePackages().isEmpty()) {
            return properties.getBasePackages();
        }
        if (event.getApplicationContext() instanceof AbstractApplicationContext ctx
                && AutoConfigurationPackages.has(ctx.getBeanFactory())) {
            return AutoConfigurationPackages.get(ctx.getBeanFactory());
        }
        log.warn("No criteria-filter.base-packages configured and no auto-configuration packages found; "
                + "no filterable entities will be discovered");
        return List.of();
    }
}
