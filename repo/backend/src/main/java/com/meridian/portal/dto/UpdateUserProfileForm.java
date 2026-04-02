package com.meridian.portal.dto;

import jakarta.validation.constraints.Size;

public class UpdateUserProfileForm {

    @Size(max = 128)
    private String employeeIdentifier;

    @Size(max = 128)
    private String contactField;

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
