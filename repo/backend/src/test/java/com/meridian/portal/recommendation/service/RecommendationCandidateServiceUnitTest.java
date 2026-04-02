package com.meridian.portal.recommendation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

class RecommendationCandidateServiceUnitTest {

    @Test
    void coldStartFallsBackToNewestWhenNoPopularCategories() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate() {
            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper, Object... args) {
                // No popular categories => fallback path.
                return List.of();
            }

            @Override
            public <T> List<T> query(String sql, RowMapper<T> rowMapper) {
                @SuppressWarnings("unchecked")
                T a = (T) new ScoredCandidate("SKU-1", "p1", "c1", BigDecimal.ONE, 0.05);
                @SuppressWarnings("unchecked")
                T b = (T) new ScoredCandidate("SKU-2", "p2", "c2", BigDecimal.TEN, 0.05);
                return List.of(a, b);
            }
        };

        RecommendationCandidateService service = new RecommendationCandidateService(jdbcTemplate);
        List<ScoredCandidate> out = service.coldStartCandidates(5);
        assertEquals(2, out.size());
        assertEquals("SKU-1", out.getFirst().sku());
    }
}
