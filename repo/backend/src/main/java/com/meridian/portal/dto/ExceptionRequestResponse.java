package com.meridian.portal.dto;

import java.time.Instant;

public record ExceptionRequestResponse(
    long id,
    String requestKey,
    String requesterUsername,
    String requestType,
    String details,
    String status,
    String decidedBy,
    String decisionComment,
    Instant decidedAt,
    Instant createdAt
) {}
