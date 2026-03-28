package com.meridian.portal.notification.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

final class NotificationDeliveryPolicy {

    private NotificationDeliveryPolicy() {}

    static int effectiveReminderCap(int systemCap, int userCap, int eventCap, int definitionCap) {
        return Math.min(systemCap, Math.min(userCap, Math.min(eventCap, definitionCap)));
    }

    static boolean isWithinDnd(LocalTime start, LocalTime end, Instant nowInstant) {
        LocalTime now = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault()).toLocalTime();
        if (start.equals(end)) {
            return false;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && now.isBefore(end);
        }
        return !now.isBefore(start) || now.isBefore(end);
    }

    static Instant nextOutsideDnd(LocalTime dndEnd, Instant nowInstant) {
        LocalDateTime now = LocalDateTime.ofInstant(nowInstant, ZoneId.systemDefault());
        LocalDateTime candidate = now.withHour(dndEnd.getHour()).withMinute(dndEnd.getMinute()).withSecond(0).withNano(0);
        if (!candidate.isAfter(now)) {
            candidate = candidate.plusDays(1);
        }
        return candidate.atZone(ZoneId.systemDefault()).toInstant();
    }
}
