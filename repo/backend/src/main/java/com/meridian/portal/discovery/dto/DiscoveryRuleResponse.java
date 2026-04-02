package com.meridian.portal.discovery.dto;

import java.time.Instant;

public record DiscoveryRuleResponse(
    long id,
    String name,
    String ruleType,
    String matchValue,
    String targetValue,
    int priority,
    boolean active,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {}
