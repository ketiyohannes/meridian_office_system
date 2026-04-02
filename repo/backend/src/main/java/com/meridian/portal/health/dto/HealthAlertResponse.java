package com.meridian.portal.health.dto;

import java.time.Instant;

public record HealthAlertResponse(
    Long id,
    String metricCode,
    int windowMinutes,
    double thresholdPercent,
    double actualPercent,
    Instant bucketStart,
    boolean resolved,
    Instant resolvedAt,
    Instant createdAt
) {}
