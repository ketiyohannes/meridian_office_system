package com.meridian.portal.notification.dto;

import jakarta.validation.constraints.NotBlank;

public record ApprovalOutcomeRequest(
    @NotBlank String username,
    @NotBlank String eventKey,
    boolean approved,
    String details
) {}
