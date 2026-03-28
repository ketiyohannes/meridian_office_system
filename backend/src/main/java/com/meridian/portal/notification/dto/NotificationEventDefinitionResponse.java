package com.meridian.portal.notification.dto;

public record NotificationEventDefinitionResponse(
    String topic,
    int reminderIntervalMinutes,
    int maxReminders,
    boolean actionable,
    boolean active
) {}
