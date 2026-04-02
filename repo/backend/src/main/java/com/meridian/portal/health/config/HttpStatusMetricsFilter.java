package com.meridian.portal.health.config;

import com.meridian.portal.health.service.HealthMonitoringService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class HttpStatusMetricsFilter extends OncePerRequestFilter {

    private final HealthMonitoringService healthMonitoringService;

    public HttpStatusMetricsFilter(HealthMonitoringService healthMonitoringService) {
        this.healthMonitoringService = healthMonitoringService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        int status = 200;
        try {
            filterChain.doFilter(request, response);
            status = response.getStatus();
        } catch (Exception ex) {
            status = 500;
            throw ex;
        } finally {
            healthMonitoringService.recordHttpStatus(status, request.getRequestURI(), Instant.now());
        }
    }
}
