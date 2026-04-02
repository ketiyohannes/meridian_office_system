package com.meridian.portal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
    @NotBlank @Size(max = 64) String username,
    @NotBlank @Size(max = 255) String title,
    String description,
    String dueAt
) {}
