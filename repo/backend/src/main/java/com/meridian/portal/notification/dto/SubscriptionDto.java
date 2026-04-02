package com.meridian.portal.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record SubscriptionDto(@NotBlank String topic, boolean subscribed) {}
