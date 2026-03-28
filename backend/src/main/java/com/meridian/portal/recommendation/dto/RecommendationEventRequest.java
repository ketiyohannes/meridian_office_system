package com.meridian.portal.recommendation.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecommendationEventRequest(
    @NotBlank String eventType,
    @Size(max = 255) String queryText,
    @Size(max = 64) String sku,
    @Size(max = 100) String categoryName,
    @Size(max = 40) String region
) {}
