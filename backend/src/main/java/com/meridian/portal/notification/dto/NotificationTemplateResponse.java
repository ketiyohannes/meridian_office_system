package com.meridian.portal.notification.dto;

public record NotificationTemplateResponse(
    String topic,
    String titleTemplate,
    String bodyTemplate,
    boolean active
) {}
