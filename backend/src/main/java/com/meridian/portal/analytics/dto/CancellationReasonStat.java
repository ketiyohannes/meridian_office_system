package com.meridian.portal.analytics.dto;

public record CancellationReasonStat(
    String reason,
    long count
) {}
