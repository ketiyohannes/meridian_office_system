package com.meridian.portal.notification.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record EventDefinitionUpdateDto(
    @Min(1) @Max(240) int reminderIntervalMinutes,
    @Min(1) @Max(3) int maxReminders,
    boolean actionable,
    boolean active
) {}
