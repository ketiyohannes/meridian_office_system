package com.meridian.portal.dto;

import com.meridian.portal.domain.RoleName;
import jakarta.validation.constraints.NotEmpty;
import java.util.HashSet;
import java.util.Set;

public class UpdateRolesForm {

    @NotEmpty
    private Set<RoleName> roles = new HashSet<>();

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }
}
