package dev.agiro.criteriafilter.web;

import org.springframework.core.Ordered;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Method;

/**
 * Secondary {@link RequestMappingHandlerMapping} that registers the dynamic
 * filter endpoints declared with {@link dev.agiro.criteriafilter.annotation.EnableFilterEndpoint}.
 */
public class FilterEndpointHandlerMapping extends RequestMappingHandlerMapping {

    public static final int ORDER = Ordered.HIGHEST_PRECEDENCE;

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    protected boolean isHandler(Class<?> beanType) {
        // Do not auto-detect controllers; endpoints are registered explicitly
        // by FilterEndpointRegistrar.
        return false;
    }

    /**
     * Registers search and schema mappings for the given adapter and path.
     */
    public void registerEndpoint(String searchPath, String schemaPath, FilterEndpointAdapter handler) {
        Method searchMethod = handler.searchMethod();
        Method schemaMethod = handler.schemaMethod();

        RequestMappingInfo searchInfo = RequestMappingInfo
                .paths(searchPath)
                .methods(RequestMethod.POST)
                .build();
        super.registerMapping(searchInfo, handler, searchMethod);

        RequestMappingInfo schemaInfo = RequestMappingInfo
                .paths(schemaPath)
                .methods(RequestMethod.GET)
                .build();
        super.registerMapping(schemaInfo, handler, schemaMethod);
    }
}
