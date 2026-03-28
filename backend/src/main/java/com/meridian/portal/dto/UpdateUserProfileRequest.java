package com.meridian.portal.dto;

import jakarta.validation.constraints.Size;

public record UpdateUserProfileRequest(
    @Size(max = 128) String employeeIdentifier,
    @Size(max = 128) String contactField
) {}
