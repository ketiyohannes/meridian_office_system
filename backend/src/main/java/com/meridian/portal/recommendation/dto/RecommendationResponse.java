package com.meridian.portal.recommendation.dto;

import java.math.BigDecimal;

public record RecommendationResponse(
    String sku,
    String name,
    String category,
    BigDecimal price,
    double score,
    boolean longTail,
    String reasonCode
) {}
