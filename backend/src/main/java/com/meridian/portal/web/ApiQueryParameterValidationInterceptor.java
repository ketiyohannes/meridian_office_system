package com.meridian.portal.web;

import com.meridian.portal.exception.ValidationException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ApiQueryParameterValidationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull Object handler
    ) {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        Set<String> allowed = extractAllowedQueryParameters(handlerMethod);
        if (allowed == null) {
            return true;
        }
        for (String key : request.getParameterMap().keySet()) {
            if ("_csrf".equals(key)) {
                continue;
            }
            if (!allowed.contains(key)) {
                throw new ValidationException("Unknown query parameter: " + key);
            }
        }
        return true;
    }

    private Set<String> extractAllowedQueryParameters(HandlerMethod handlerMethod) {
        Set<String> allowed = new HashSet<>();
        for (MethodParameter parameter : handlerMethod.getMethodParameters()) {
            RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
            if (requestParam == null) {
                continue;
            }

            if (isRequestParamMap(parameter)) {
                // Dynamic query-param map is explicitly declared by endpoint.
                // Returning all known params is not possible in this case, so skip strict blocking.
                return null;
            }

            String name = requestParam.name();
            if (name == null || name.isBlank()) {
                name = requestParam.value();
            }
            if ((name == null || name.isBlank()) && parameter.getParameterName() != null) {
                name = parameter.getParameterName();
            }
            if (name != null && !name.isBlank()) {
                allowed.add(name);
            }
        }
        return allowed;
    }

    private boolean isRequestParamMap(MethodParameter parameter) {
        if (!Map.class.isAssignableFrom(parameter.getParameterType())) {
            return false;
        }
        Type genericType = parameter.getGenericParameterType();
        if (!(genericType instanceof ParameterizedType parameterizedType)) {
            return true;
        }
        Type[] args = parameterizedType.getActualTypeArguments();
        return args.length == 2 && args[0].getTypeName().contains("String");
    }
}
