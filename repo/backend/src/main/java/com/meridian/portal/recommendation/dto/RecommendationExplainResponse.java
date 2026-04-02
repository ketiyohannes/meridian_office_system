package com.meridian.portal.recommendation.dto;

public record RecommendationExplainResponse(
    String sku,
    String name,
    String category,
    double score,
    boolean longTail,
    String reason
) {}
