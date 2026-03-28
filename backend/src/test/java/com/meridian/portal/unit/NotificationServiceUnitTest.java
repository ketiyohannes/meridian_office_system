package com.meridian.portal.unit;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
class NotificationServiceUnitTest {

    @Test
    void rejectsNegativePage() {
        NotificationService notificationService = new NotificationService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new ObjectMapper()
        );
        assertThrows(ValidationException.class, () -> notificationService.myNotifications(null, -1, 10));
    }

    @Test
    void rejectsOversizedPageSize() {
        NotificationService notificationService = new NotificationService(
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            new ObjectMapper()
        );
        assertThrows(ValidationException.class, () -> notificationService.myNotifications(null, 0, 101));
    }
}
