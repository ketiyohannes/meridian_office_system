package com.meridian.portal.dto;

import java.time.Instant;

public record TaskResponse(
    long id,
    String username,
    String title,
    String description,
    Instant dueAt,
    String status,
    String createdBy,
    Instant completedAt,
    Instant createdAt
) {}
