package com.meridian.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExceptionRequest(
    @NotBlank @Size(max = 64) String requestType,
    @NotBlank @Size(max = 2000) String details
) {}
