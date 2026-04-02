package com.meridian.portal.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.service.AdminAuditLogService;
import org.junit.jupiter.api.Test;

class AdminAuditLogServiceUnitTest {

    @Test
    void rejectsNegativePage() {
        AdminAuditLogService service = new AdminAuditLogService(null);
        assertThrows(ValidationException.class, () -> service.search(null, null, null, null, null, null, -1, 10));
    }

    @Test
    void rejectsOversizedPageSize() {
        AdminAuditLogService service = new AdminAuditLogService(null);
        assertThrows(ValidationException.class, () -> service.search(null, null, null, null, null, null, 0, 101));
    }
}
