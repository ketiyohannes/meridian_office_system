package com.meridian.portal.integration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.meridian.portal.discovery.repository.SearchQueryEventRepository;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class DiscoveryTrendingBoundaryIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private SearchQueryEventRepository searchQueryEventRepository;

    @Test
    void trendingIncludesOnlyEventsWithinExactSevenDayWindow() throws Exception {
        Instant cutoff = Instant.now().minusSeconds(7L * 24 * 60 * 60);
        Timestamp insideWindow = Timestamp.from(cutoff.plusSeconds(24L * 60 * 60));
        Timestamp outsideWindow = Timestamp.from(cutoff.minusSeconds(24L * 60 * 60));

        jdbcTemplate.update(
            """
            INSERT INTO search_query_events (username, query_text, created_at)
            VALUES (?, ?, ?)
            """,
            "admin",
            "within-seven-days",
            insideWindow
        );
        jdbcTemplate.update(
            """
            INSERT INTO search_query_events (username, query_text, created_at)
            VALUES (?, ?, ?)
            """,
            "admin",
            "outside-seven-days",
            outsideWindow
        );

        List<String> trending = searchQueryEventRepository.findTrendingQueries(cutoff, 20);
        assertTrue(trending.contains("within-seven-days"));
        assertFalse(trending.contains("outside-seven-days"));
    }
}
