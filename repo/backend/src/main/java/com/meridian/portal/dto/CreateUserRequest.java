package com.meridian.portal.dto;

import com.meridian.portal.domain.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record CreateUserRequest(
    @NotBlank @Size(min = 3, max = 64) String username,
    @NotBlank @Size(min = 12, max = 128) String password,
    @NotEmpty Set<RoleName> roles,
    boolean enabled,
    @Size(max = 128) String employeeIdentifier,
    @Size(max = 128) String contactField
) {}
