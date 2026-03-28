package com.meridian.portal.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class RecommendationExposureServiceUnitTest {

    @Test
    void dailyImpressionMapReturnsMappedRows() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                @SuppressWarnings("unchecked")
                T a = (T) Map.entry("SKU-1", 3);
                @SuppressWarnings("unchecked")
                T b = (T) Map.entry("SKU-2", 7);
                return List.of(a, b);
            }
        };

        RecommendationExposureService service = new RecommendationExposureService(jdbcTemplate);
        Map<String, Integer> map = service.dailyImpressionMap("GLOBAL");
        assertEquals(2, map.size());
        assertEquals(3, map.get("SKU-1"));
        assertEquals(7, map.get("SKU-2"));
    }

    @Test
    void recordExposureWithinCapWritesExposureAndDailyCount() {
        AtomicInteger updates = new AtomicInteger(0);
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public int update(String sql, Object... args) {
                updates.incrementAndGet();
                return 1;
            }
        };

        RecommendationExposureService service = new RecommendationExposureService(jdbcTemplate);
        service.recordExposureWithinCap(
            "admin",
            "GLOBAL",
            "HOME",
            List.of(new PickedCandidate("SKU-1", "n", "c", BigDecimal.ONE, 0.2, false, "r")),
            200
        );

        assertEquals(2, updates.get());
    }
}
