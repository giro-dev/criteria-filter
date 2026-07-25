package dev.agiro.criteriafilter.web;

import dev.agiro.criteriafilter.annotation.EnableFilterEndpoint;
import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.validation.FilterValidator;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Scans bean definitions for {@link EnableFilterEndpoint} and registers the
 * filter endpoints with {@link FilterEndpointHandlerMapping}.
 */
public class FilterEndpointRegistrar implements SmartInitializingSingleton {

    private final ApplicationContext applicationContext;
    private final FilterEndpointHandlerMapping handlerMapping;
    private final FilterValidator filterValidator;
    private final CriteriaRepositoryRegistry repositoryRegistry;
    private final FilterMetadataRegistry metadataRegistry;
    private final FilterInterceptorChain interceptorChain;

    public FilterEndpointRegistrar(ApplicationContext applicationContext,
                                    FilterEndpointHandlerMapping handlerMapping,
                                    FilterValidator filterValidator,
                                    CriteriaRepositoryRegistry repositoryRegistry,
                                    FilterMetadataRegistry metadataRegistry,
                                    FilterInterceptorChain interceptorChain) {
        this.applicationContext = applicationContext;
        this.handlerMapping = handlerMapping;
        this.filterValidator = filterValidator;
        this.repositoryRegistry = repositoryRegistry;
        this.metadataRegistry = metadataRegistry;
        this.interceptorChain = interceptorChain;
    }

    @Override
    public void afterSingletonsInstantiated() {
        for (String beanName : applicationContext.getBeanDefinitionNames()) {
            Class<?> beanType = applicationContext.getType(beanName);
            if (beanType == null) {
                continue;
            }
            EnableFilterEndpoint annotation = AnnotatedElementUtils.findMergedAnnotation(
                    beanType, EnableFilterEndpoint.class);
            if (annotation == null) {
                continue;
            }
            registerFor(beanType, annotation);
        }
    }

    private void registerFor(Class<?> beanType, EnableFilterEndpoint annotation) {
        String basePath = resolveBasePath(beanType);
        String searchPath = basePath + "/" + annotation.searchPath();
        String schemaPath = basePath + "/" + annotation.schemaPath();

        // Avoid double slashes and trailing slashes
        searchPath = normalize(searchPath);
        schemaPath = normalize(schemaPath);

        FilterEndpointAdapter adapter = new FilterEndpointAdapter(
                annotation.entity(),
                filterValidator,
                repositoryRegistry,
                metadataRegistry,
                interceptorChain
        );

        handlerMapping.registerEndpoint(
                searchPath,
                annotation.includeSchema() ? schemaPath : null,
                adapter
        );
    }

    private String resolveBasePath(Class<?> beanType) {
        RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(beanType, RequestMapping.class);
        if (mapping == null || mapping.value().length == 0) {
            return "";
        }
        String path = mapping.value()[0];
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    private String normalize(String path) {
        path = path.replaceAll("/+", "/");
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path;
    }
}
