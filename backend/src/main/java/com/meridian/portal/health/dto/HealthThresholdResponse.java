package com.meridian.portal.health.dto;

public record HealthThresholdResponse(
    String metricCode,
    int windowMinutes,
    double thresholdPercent,
    boolean enabled
) {}
