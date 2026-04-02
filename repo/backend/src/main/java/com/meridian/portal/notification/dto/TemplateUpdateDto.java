package com.meridian.portal.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TemplateUpdateDto(
    @NotBlank @Size(max = 255) String titleTemplate,
    @NotBlank String bodyTemplate,
    boolean active
) {}
