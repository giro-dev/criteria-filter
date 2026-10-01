package dev.agiro.criteriafilter.autoconfigure;

import dev.agiro.criteriafilter.web.FilterEndpointAdapter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.api.AbstractOpenApiResource;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.util.List;

/**
 * Makes filter endpoints registered dynamically by {@code @EnableFilterEndpoint}
 * visible to springdoc by exposing {@link FilterEndpointAdapter} as an additional
 * controller type to scan.
 */
@AutoConfiguration(after = CriteriaFilterAutoConfiguration.class)
@ConditionalOnClass(AbstractOpenApiResource.class)
public class CriteriaFilterOpenApiAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "criteriaFilterOpenApiRegistrar")
    public SmartInitializingSingleton criteriaFilterOpenApiRegistrar() {
        return () -> AbstractOpenApiResource.addRestControllers(FilterEndpointAdapter.class);
    }

    @Bean
    @ConditionalOnMissingBean(name = "criteriaFilterOperationCustomizer")
    public OperationCustomizer criteriaFilterOperationCustomizer() {
        return (operation, handlerMethod) -> {
            Object handler = handlerMethod.getBean();
            if (handler instanceof FilterEndpointAdapter adapter
                    && adapter.sourceControllerType() != null) {
                Tag tag = AnnotatedElementUtils.findMergedAnnotation(adapter.sourceControllerType(), Tag.class);
                if (tag != null && !tag.name().isBlank()) {
                    operation.setTags(List.of(tag.name()));
                }
            }
            return operation;
        };
    }
}
