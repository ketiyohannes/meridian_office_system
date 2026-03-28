package com.meridian.portal.health.controller;

import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.health.dto.HealthAlertResponse;
import com.meridian.portal.health.dto.HealthErrorLogResponse;
import com.meridian.portal.health.dto.HealthSummaryResponse;
import com.meridian.portal.health.dto.HealthThresholdResponse;
import com.meridian.portal.health.dto.UpdateHealthThresholdRequest;
import com.meridian.portal.health.service.HealthMonitoringService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/health")
@PreAuthorize("hasRole('ADMIN')")
public class AdminHealthApiController {

    private final HealthMonitoringService healthMonitoringService;

    public AdminHealthApiController(HealthMonitoringService healthMonitoringService) {
        this.healthMonitoringService = healthMonitoringService;
    }

    @GetMapping("/summary")
    public HealthSummaryResponse summary() {
        return healthMonitoringService.summary();
    }

    @GetMapping("/thresholds")
    public List<HealthThresholdResponse> thresholds() {
        return healthMonitoringService.thresholds();
    }

    @PutMapping("/thresholds/{metricCode}")
    public HealthThresholdResponse updateThreshold(
        @PathVariable String metricCode,
        @Valid @RequestBody UpdateHealthThresholdRequest body
    ) {
        return healthMonitoringService.updateThreshold(metricCode, body);
    }

    @GetMapping("/alerts")
    public PagedResponse<HealthAlertResponse> alerts(
        @RequestParam(name = "resolved", required = false) Boolean resolved,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return healthMonitoringService.alerts(resolved, page, size);
    }

    @PutMapping("/alerts/{alertId}/resolve")
    public void resolve(@PathVariable long alertId) {
        healthMonitoringService.resolveAlert(alertId);
    }

    @GetMapping("/errors")
    public PagedResponse<HealthErrorLogResponse> errors(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return healthMonitoringService.errors(page, size);
    }
}
