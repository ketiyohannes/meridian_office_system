package com.meridian.portal.dto;

import java.time.Instant;

public record AdminAuditLogResponse(
    Long id,
    String actorUsername,
    String action,
    String targetType,
    Long targetId,
    String targetUsername,
    String details,
    Instant createdAt
) {}
