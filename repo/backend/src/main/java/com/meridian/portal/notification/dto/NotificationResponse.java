package com.meridian.portal.notification.dto;

import java.time.Instant;

public record NotificationResponse(
    Long id,
    String topic,
    String title,
    String body,
    Instant sentAt,
    Instant readAt,
    String status
) {}
