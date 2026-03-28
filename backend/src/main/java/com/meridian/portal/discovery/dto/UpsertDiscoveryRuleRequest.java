package com.meridian.portal.discovery.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpsertDiscoveryRuleRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 32) String ruleType,
    @Size(max = 120) String matchValue,
    @NotBlank @Size(max = 120) String targetValue,
    @Min(1) @Max(1000) int priority,
    boolean active
) {}
