package com.meridian.portal.config;

import com.meridian.portal.web.ApiQueryParameterValidationInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ApiQueryParameterValidationInterceptor apiQueryParameterValidationInterceptor;

    public WebMvcConfig(ApiQueryParameterValidationInterceptor apiQueryParameterValidationInterceptor) {
        this.apiQueryParameterValidationInterceptor = apiQueryParameterValidationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiQueryParameterValidationInterceptor)
            .addPathPatterns("/api/**");
    }
}
