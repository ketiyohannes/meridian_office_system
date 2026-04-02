package com.meridian.portal.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class NotificationDeliveryPolicyUnitTest {

    @Test
    void effectiveReminderCapUsesMinimumAcrossAllCaps() {
        assertEquals(1, NotificationDeliveryPolicy.effectiveReminderCap(3, 2, 1, 3));
        assertEquals(3, NotificationDeliveryPolicy.effectiveReminderCap(3, 5, 4, 6));
    }

    @Test
    void dndWindowHandlesNormalAndOvernightRanges() {
        Instant daytime = Instant.parse("2026-03-27T10:00:00Z");
        Instant nighttime = Instant.parse("2026-03-27T20:30:00Z");

        assertTrue(NotificationDeliveryPolicy.isWithinDnd(LocalTime.of(9, 0), LocalTime.of(17, 0), daytime));
        assertFalse(NotificationDeliveryPolicy.isWithinDnd(LocalTime.of(9, 0), LocalTime.of(17, 0), nighttime));
        assertTrue(NotificationDeliveryPolicy.isWithinDnd(LocalTime.of(21, 0), LocalTime.of(7, 0), nighttime));
    }

    @Test
    void dndDisabledWhenStartEqualsEnd() {
        assertFalse(NotificationDeliveryPolicy.isWithinDnd(
            LocalTime.of(21, 0),
            LocalTime.of(21, 0),
            Instant.parse("2026-03-27T22:00:00Z")
        ));
    }

    @Test
    void nextOutsideDndMovesToFutureBoundary() {
        Instant now = Instant.parse("2026-03-27T20:30:00Z");
        Instant next = NotificationDeliveryPolicy.nextOutsideDnd(LocalTime.of(7, 0), now);
        assertTrue(next.isAfter(now));
    }
}
