package com.meridian.portal.discovery.dto;

public record ActiveDiscoveryRule(
    String ruleType,
    String matchValue,
    String targetValue,
    int priority
) {}
