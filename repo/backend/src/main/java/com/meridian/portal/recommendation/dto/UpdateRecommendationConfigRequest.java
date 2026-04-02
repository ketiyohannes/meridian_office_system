package com.meridian.portal.recommendation.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateRecommendationConfigRequest(
    @NotBlank String value
) {}
