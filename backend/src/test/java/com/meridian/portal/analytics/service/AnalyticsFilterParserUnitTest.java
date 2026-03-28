package com.meridian.portal.analytics.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meridian.portal.analytics.dto.KpiFilters;
import com.meridian.portal.exception.ValidationException;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class AnalyticsFilterParserUnitTest {

    @Test
    void parsesAndTrimsValidFilters() {
        KpiFilters filters = AnalyticsFilterParser.parse(
            "2026-03-01T00:00:00Z",
            "2026-03-27T00:00:00Z",
            "  SKU-1001 ",
            " Electronics ",
            " STORE ",
            " ROLE_ANALYST ",
            " ET-ADD "
        );

        assertEquals("SKU-1001", filters.product());
        assertEquals("Electronics", filters.category());
        assertEquals("STORE", filters.channel());
        assertEquals("ROLE_ANALYST", filters.role());
        assertEquals("ET-ADD", filters.region());
    }

    @Test
    void parsesLocalDateTimeFallback() {
        Instant parsed = AnalyticsFilterParser.parseDateTime(
            "2026-03-27T10:15:30",
            "from",
            Instant.parse("2026-01-01T00:00:00Z")
        );
        assertTrue(parsed.isAfter(Instant.parse("2026-03-27T00:00:00Z")));
    }

    @Test
    void rejectsInvalidRangeAndInvalidDate() {
        assertThrows(ValidationException.class, () -> AnalyticsFilterParser.parse(
            "2026-03-27T01:00:00Z",
            "2026-03-27T00:00:00Z",
            null, null, null, null, null
        ));

        assertThrows(ValidationException.class, () -> AnalyticsFilterParser.parseDateTime(
            "not-a-date",
            "from",
            Instant.now()
        ));
    }

    @Test
    void blanksNormalizeToNull() {
        KpiFilters filters = AnalyticsFilterParser.parse(
            "2026-03-01T00:00:00Z",
            "2026-03-27T00:00:00Z",
            " ",
            "",
            null,
            "   ",
            "\t"
        );
        assertNull(filters.product());
        assertNull(filters.category());
        assertNull(filters.channel());
        assertNull(filters.role());
        assertNull(filters.region());
    }
}
