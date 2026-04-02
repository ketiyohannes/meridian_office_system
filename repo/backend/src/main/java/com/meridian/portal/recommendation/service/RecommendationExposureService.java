package com.meridian.portal.recommendation.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class RecommendationExposureService {

    private final JdbcTemplate jdbcTemplate;

    RecommendationExposureService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    Map<String, Integer> dailyImpressionMap(String region) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<Map.Entry<String, Integer>> rows = jdbcTemplate.query(
            """
            SELECT sku, impressions
              FROM recommendation_daily_impressions
             WHERE day_bucket = ?
               AND region_code = ?
            """,
            (rs, i) -> Map.entry(rs.getString("sku"), rs.getInt("impressions")),
            today,
            region
        );
        Map<String, Integer> map = new HashMap<>();
        for (Map.Entry<String, Integer> row : rows) {
            map.put(row.getKey(), row.getValue());
        }
        return map;
    }

    List<PickedCandidate> recordExposureWithinCap(String username, String region, String surface, List<PickedCandidate> list, int perRegionCap) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        List<PickedCandidate> accepted = new ArrayList<>();
        for (PickedCandidate item : list) {
            if (!reserveImpression(today, region, item.sku(), perRegionCap)) {
                continue;
            }

            jdbcTemplate.update(
                """
                INSERT INTO recommendation_exposures (username, sku, region_code, surface)
                VALUES (?, ?, ?, ?)
                """,
                username,
                item.sku(),
                region,
                surface
            );
            accepted.add(item);
        }
        return accepted;
    }

    private boolean reserveImpression(LocalDate dayBucket, String region, String sku, int perRegionCap) {
        int updated = jdbcTemplate.update(
            """
            UPDATE recommendation_daily_impressions
               SET impressions = impressions + 1
             WHERE day_bucket = ?
               AND region_code = ?
               AND sku = ?
               AND impressions < ?
            """,
            dayBucket,
            region,
            sku,
            perRegionCap
        );
        if (updated > 0) {
            return true;
        }

        int inserted = jdbcTemplate.update(
            """
            INSERT IGNORE INTO recommendation_daily_impressions (day_bucket, region_code, sku, impressions)
            VALUES (?, ?, ?, 1)
            """,
            dayBucket,
            region,
            sku
        );
        if (inserted > 0) {
            return true;
        }

        updated = jdbcTemplate.update(
            """
            UPDATE recommendation_daily_impressions
               SET impressions = impressions + 1
             WHERE day_bucket = ?
               AND region_code = ?
               AND sku = ?
               AND impressions < ?
            """,
            dayBucket,
            region,
            sku,
            perRegionCap
        );
        return updated > 0;
    }

    Instant dedupeCutoff(int dedupeHours) {
        return Instant.now().minusSeconds(dedupeHours * 60L * 60L);
    }
}
