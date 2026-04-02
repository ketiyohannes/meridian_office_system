package com.meridian.portal.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record UserNotificationPreferenceDto(
    @NotBlank String dndStart,
    @NotBlank String dndEnd,
    @Min(1) @Max(3) int maxRemindersPerEvent
) {}
