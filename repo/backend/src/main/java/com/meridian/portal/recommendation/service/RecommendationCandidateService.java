package com.meridian.portal.recommendation.service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class RecommendationCandidateService {

    private final JdbcTemplate jdbcTemplate;

    RecommendationCandidateService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    List<ScoredCandidate> scoredCandidates(String username, int scoringWindowDays) {
        Instant cutoff = Instant.now().minusSeconds(scoringWindowDays * 24L * 60L * 60L);
        return jdbcTemplate.query(
            """
            SELECT p.sku, p.name, c.name AS category_name, p.price, p.posted_at,
                   COALESCE(uc.cat_score, 0) + COALESCE(ps.pop_score, 0) AS total_score
              FROM products p
              JOIN categories c ON c.id = p.category_id
              LEFT JOIN (
                    SELECT category_name, SUM(
                        CASE event_type
                            WHEN 'SEARCH' THEN 1
                            WHEN 'VIEW' THEN 2
                            WHEN 'ADD_TO_CART' THEN 3
                            WHEN 'PURCHASE' THEN 4
                            ELSE 0
                        END
                    ) AS cat_score
                      FROM recommendation_events
                     WHERE username = ?
                       AND created_at >= ?
                       AND category_name IS NOT NULL
                     GROUP BY category_name
              ) uc ON uc.category_name = c.name
              LEFT JOIN (
                    SELECT sku, SUM(
                        CASE event_type
                            WHEN 'VIEW' THEN 1
                            WHEN 'ADD_TO_CART' THEN 2
                            WHEN 'PURCHASE' THEN 3
                            ELSE 0
                        END
                    ) AS pop_score
                      FROM recommendation_events
                     WHERE created_at >= ?
                       AND sku IS NOT NULL
                     GROUP BY sku
              ) ps ON ps.sku = p.sku
             WHERE p.enabled = 1
             ORDER BY total_score DESC, p.posted_at DESC, p.sku ASC
             LIMIT 500
            """,
            (rs, i) -> new ScoredCandidate(
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                rs.getDouble("total_score")
            ),
            username,
            Timestamp.from(cutoff),
            Timestamp.from(cutoff)
        );
    }

    List<ScoredCandidate> coldStartCandidates(int categoryLimit) {
        Instant cutoff = Instant.now().minusSeconds(30L * 24 * 60 * 60);
        List<String> categories = jdbcTemplate.query(
            """
            SELECT category_name
              FROM order_facts
             WHERE ordered_at >= ?
             GROUP BY category_name
             ORDER BY COUNT(*) DESC, category_name ASC
             LIMIT ?
            """,
            (rs, i) -> rs.getString("category_name"),
            Timestamp.from(cutoff),
            categoryLimit
        );

        if (RecommendationSelectionPolicy.shouldUseNewestFallback(categories)) {
            return newestProducts();
        }

        List<ScoredCandidate> result = new ArrayList<>();
        for (String category : categories) {
            result.addAll(jdbcTemplate.query(
                """
                SELECT p.sku, p.name, c.name AS category_name, p.price
                  FROM products p
                  JOIN categories c ON c.id = p.category_id
                 WHERE p.enabled = 1
                   AND c.name = ?
                 ORDER BY p.posted_at DESC, p.sku ASC
                 LIMIT 50
                """,
                (rs, i) -> new ScoredCandidate(
                    rs.getString("sku"),
                    rs.getString("name"),
                    rs.getString("category_name"),
                    rs.getBigDecimal("price"),
                    0.15
                ),
                category
            ));
        }
        return result;
    }

    List<ScoredCandidate> longTailCandidates(String region, int longTailWindowDays) {
        Instant cutoff = Instant.now().minusSeconds(longTailWindowDays * 24L * 60L * 60L);
        return jdbcTemplate.query(
            """
            SELECT p.sku, p.name, c.name AS category_name, p.price,
                   COALESCE(e.impressions, 0) AS impressions
              FROM products p
              JOIN categories c ON c.id = p.category_id
              LEFT JOIN (
                    SELECT sku, COUNT(*) AS impressions
                      FROM recommendation_exposures
                     WHERE region_code = ?
                       AND created_at >= ?
                     GROUP BY sku
              ) e ON e.sku = p.sku
             WHERE p.enabled = 1
             ORDER BY COALESCE(e.impressions, 0) ASC, p.posted_at DESC, p.sku ASC
             LIMIT 400
            """,
            (rs, i) -> new ScoredCandidate(
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                0.2
            ),
            region,
            Timestamp.from(cutoff)
        );
    }

    List<ScoredCandidate> newestProducts() {
        return jdbcTemplate.query(
            """
            SELECT p.sku, p.name, c.name AS category_name, p.price
              FROM products p
              JOIN categories c ON c.id = p.category_id
             WHERE p.enabled = 1
             ORDER BY p.posted_at DESC, p.sku ASC
             LIMIT 200
            """,
            (rs, i) -> new ScoredCandidate(
                rs.getString("sku"),
                rs.getString("name"),
                rs.getString("category_name"),
                rs.getBigDecimal("price"),
                0.05
            )
        );
    }
}
