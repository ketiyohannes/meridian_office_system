package com.meridian.portal.recommendation.dto;

public record RecommendationExposureStatResponse(
    String sku,
    String region,
    String surface,
    long impressions
) {}
