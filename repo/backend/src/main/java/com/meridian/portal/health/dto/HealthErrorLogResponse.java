package com.meridian.portal.health.dto;

import java.time.Instant;

public record HealthErrorLogResponse(
    Long id,
    int statusCode,
    String requestPath,
    String message,
    String detailsJson,
    Instant occurredAt
) {}
