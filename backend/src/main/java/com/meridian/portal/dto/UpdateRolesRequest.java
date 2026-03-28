package com.meridian.portal.dto;

import com.meridian.portal.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.Set;

public record UpdateRolesRequest(@NotEmpty Set<RoleName> roles) {}
