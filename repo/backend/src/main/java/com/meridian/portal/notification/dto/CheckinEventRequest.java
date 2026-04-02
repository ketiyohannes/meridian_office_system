package com.meridian.portal.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckinEventRequest(
    @NotBlank String username,
    @NotBlank String eventKey,
    @NotBlank String opensAt,
    @NotBlank String cutoffAt,
    String missedAt
) {}
