package com.meridian.portal.dto;

import com.meridian.portal.domain.RoleName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

public class CreateUserForm {

    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    @NotBlank
    @Size(min = 12, max = 128)
    private String password;

    @NotBlank
    @Size(min = 12, max = 128)
    private String confirmPassword;

    @NotEmpty
    private Set<RoleName> roles = new HashSet<>();

    private boolean enabled = true;
    @Size(max = 128)
    private String employeeIdentifier;
    @Size(max = 128)
    private String contactField;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }

    public Set<RoleName> getRoles() {
        return roles;
    }

    public void setRoles(Set<RoleName> roles) {
        this.roles = roles;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getEmployeeIdentifier() {
        return employeeIdentifier;
    }

    public void setEmployeeIdentifier(String employeeIdentifier) {
        this.employeeIdentifier = employeeIdentifier;
    }

    public String getContactField() {
        return contactField;
    }

    public void setContactField(String contactField) {
        this.contactField = contactField;
    }
}
