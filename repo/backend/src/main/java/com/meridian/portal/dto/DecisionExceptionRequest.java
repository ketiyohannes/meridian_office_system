package com.meridian.portal.dto;

import jakarta.validation.constraints.Size;

public record DecisionExceptionRequest(
    boolean approved,
    @Size(max = 500) String comment
) {}
