package com.meridian.portal.domain;

public enum RoleName {
    ADMIN,
    OPS_MANAGER,
    MERCHANDISER,
    ANALYST,
    REGULAR_USER;

    public String authority() {
        return "ROLE_" + name();
    }
}
