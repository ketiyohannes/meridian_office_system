package com.meridian.portal.health.dto;

import java.time.Instant;

public record HealthSummaryResponse(
    String metricCode,
    int windowMinutes,
    double thresholdPercent,
    long totalRequests,
    long errorRequests,
    double actualPercent,
    boolean alerting,
    Instant evaluatedAt
) {}
