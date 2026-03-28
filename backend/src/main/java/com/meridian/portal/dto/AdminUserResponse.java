package com.meridian.portal.dto;

import java.time.Instant;
import java.util.Set;

public record AdminUserResponse(
    Long id,
    String username,
    boolean enabled,
    Instant lockedUntil,
    String maskedEmployeeIdentifier,
    String maskedContactField,
    Set<String> roles,
    Instant createdAt,
    Instant updatedAt
) {}
