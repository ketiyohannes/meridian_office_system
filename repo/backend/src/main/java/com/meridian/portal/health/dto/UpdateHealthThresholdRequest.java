package com.meridian.portal.health.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateHealthThresholdRequest(
    @Min(1) @Max(240) int windowMinutes,
    @DecimalMin("0.001") @DecimalMax("100.000") double thresholdPercent,
    boolean enabled
) {}
