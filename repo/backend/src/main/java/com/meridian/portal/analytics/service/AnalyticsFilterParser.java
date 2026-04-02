package com.meridian.portal.analytics.service;

import com.meridian.portal.analytics.dto.KpiFilters;
import com.meridian.portal.exception.ValidationException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

final class AnalyticsFilterParser {

    private AnalyticsFilterParser() {}

    static KpiFilters parse(
        String fromRaw,
        String toRaw,
        String product,
        String category,
        String channel,
        String role,
        String region
    ) {
        Instant from = parseDateTime(fromRaw, "from", LocalDate.now().minusDays(7).atStartOfDay(ZoneId.systemDefault()).toInstant());
        Instant to = parseDateTime(toRaw, "to", Instant.now());
        if (from.isAfter(to)) {
            throw new ValidationException("from must be before or equal to to");
        }
        return new KpiFilters(
            from,
            to,
            trimToNull(product),
            trimToNull(category),
            trimToNull(channel),
            trimToNull(role),
            trimToNull(region)
        );
    }

    static Instant parseDateTime(String raw, String field, Instant fallback) {
        if (!hasText(raw)) {
            return fallback;
        }

        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException ignored) {
        }

        try {
            LocalDateTime local = LocalDateTime.parse(raw.trim(), DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            return local.atZone(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException ex) {
            throw new ValidationException("Invalid datetime for " + field);
        }
    }

    static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    static String trimToNull(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
