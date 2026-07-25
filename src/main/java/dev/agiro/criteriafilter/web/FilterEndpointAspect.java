package dev.agiro.criteriafilter.web;

import dev.agiro.criteriafilter.annotation.FilterSchema;
import dev.agiro.criteriafilter.annotation.FilterSearch;
import dev.agiro.criteriafilter.interceptor.FilterInterceptorChain;
import dev.agiro.criteriafilter.metamodel.FilterMetadataRegistry;
import dev.agiro.criteriafilter.model.FilterRequest;
import dev.agiro.criteriafilter.repository.CriteriaRepository;
import dev.agiro.criteriafilter.repository.CriteriaRepositoryRegistry;
import dev.agiro.criteriafilter.repository.PageRequest;
import dev.agiro.criteriafilter.validation.FilterValidator;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;

/**
 * AOP support for {@link FilterSearch} and {@link FilterSchema} method-level annotations.
 *
 * <p>When {@code executeDefault = true} (default), the aspect intercepts the
 * annotated method and runs the standard filter logic, ignoring the method body.
 * When {@code executeDefault = false}, the method is executed normally.
 */
@Aspect
public class FilterEndpointAspect {

    private final FilterValidator filterValidator;
    private final CriteriaRepositoryRegistry repositoryRegistry;
    private final FilterMetadataRegistry metadataRegistry;
    private final FilterInterceptorChain interceptorChain;

    public FilterEndpointAspect(FilterValidator filterValidator,
                                 CriteriaRepositoryRegistry repositoryRegistry,
                                 FilterMetadataRegistry metadataRegistry,
                                 FilterInterceptorChain interceptorChain) {
        this.filterValidator = filterValidator;
        this.repositoryRegistry = repositoryRegistry;
        this.metadataRegistry = metadataRegistry;
        this.interceptorChain = interceptorChain;
    }

    @SuppressWarnings("unchecked")
    @Around("@annotation(dev.agiro.criteriafilter.annotation.FilterSearch)")
    public Object handleFilterSearch(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        FilterSearch annotation = method.getAnnotation(FilterSearch.class);
        if (annotation == null || !annotation.executeDefault()) {
            return joinPoint.proceed();
        }

        Class<Object> entityType = (Class<Object>) annotation.entity();
        Object[] args = joinPoint.getArgs();

        FilterRequest request = findFilterRequest(args);
        PageRequest page = resolvePageRequest(args, signature);

        filterValidator.validate(request, entityType);
        CriteriaRepository<Object> repository = (CriteriaRepository<Object>) repositoryRegistry.resolve(entityType);
        
        return interceptorChain.execute(entityType, request, page, repository);
    }

    @Around("@annotation(dev.agiro.criteriafilter.annotation.FilterSchema)")
    public Object handleFilterSchema(ProceedingJoinPoint joinPoint) throws Throwable {
        Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
        FilterSchema annotation = method.getAnnotation(FilterSchema.class);
        if (annotation == null || !annotation.executeDefault()) {
            return joinPoint.proceed();
        }

        Class<?> entityType = annotation.entity();
        return FilterSchemaResponse.from(metadataRegistry.require(entityType));
    }

    private FilterRequest findFilterRequest(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof FilterRequest) {
                return (FilterRequest) arg;
            }
        }
        throw new IllegalArgumentException(
                "@FilterSearch method must declare a FilterRequest parameter");
    }

    private PageRequest resolvePageRequest(Object[] args, MethodSignature signature) {
        for (Object arg : args) {
            if (arg instanceof PageRequest) {
                return (PageRequest) arg;
            }
        }

        int page = 0;
        int size = 20;
        String[] names = signature.getParameterNames();
        for (int i = 0; i < args.length; i++) {
            if (args[i] instanceof Integer) {
                if (names != null && i < names.length) {
                    if ("page".equals(names[i])) {
                        page = (Integer) args[i];
                    } else if ("size".equals(names[i])) {
                        size = (Integer) args[i];
                    }
                }
            }
        }
        return new PageRequest(page, size);
    }
}
