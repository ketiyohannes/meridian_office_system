package com.meridian.portal.analytics.dto;

import java.time.Instant;

public record KpiFilters(
    Instant from,
    Instant to,
    String product,
    String category,
    String channel,
    String role,
    String region
) {}
