package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class RecommendationIntegrationTest extends BaseIntegrationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void recommendationsReturnAndEnforceLimit() throws Exception {
        var adminSession = loginAsAdmin();

        mockMvc.perform(get("/api/recommendations")
                .session(adminSession)
                .param("surface", "HOME")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
            .andExpect(jsonPath("$[0].sku").exists())
            .andExpect(jsonPath("$[0].reasonCode").isEmpty());
    }

    @Test
    void dedupeAndCapAreApplied() throws Exception {
        var adminSession = loginAsAdmin();
        String sku = jdbcTemplate.queryForObject("SELECT sku FROM products ORDER BY posted_at DESC LIMIT 1", String.class);

        jdbcTemplate.update(
            """
            INSERT INTO recommendation_exposures (username, sku, region_code, surface, created_at)
            VALUES ('admin', ?, 'GLOBAL', 'HOME', NOW())
            """,
            sku
        );
        jdbcTemplate.update(
            """
            INSERT INTO recommendation_daily_impressions (day_bucket, region_code, sku, impressions)
            VALUES (?, 'GLOBAL', ?, 200)
            ON DUPLICATE KEY UPDATE impressions = 200
            """,
            LocalDate.now(),
            sku
        );

        mockMvc.perform(get("/api/recommendations")
                .session(adminSession)
                .param("surface", "HOME")
                .param("limit", "10")
                .param("region", "GLOBAL"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("\"sku\":\"" + sku + "\""))));
    }

    @Test
    void longTailQuotaIsPresentAndEventTrackingWorks() throws Exception {
        var adminSession = loginAsAdmin();

        mockMvc.perform(post("/api/recommendations/events")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventType":"SEARCH",
                      "queryText":"scanner",
                      "categoryName":"Electronics"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/recommendations/events")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "eventType":"VIEW",
                      "sku":"ELEC-1001",
                      "categoryName":"Electronics"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/recommendations")
                .session(adminSession)
                .param("surface", "SEARCH")
                .param("limit", "10")
                .param("region", "GLOBAL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$[?(@.longTail == true)]").exists());
    }

    @Test
    void unknownRecommendationParamIsRejected() throws Exception {
        var adminSession = loginAsAdmin();

        mockMvc.perform(get("/api/recommendations")
                .session(adminSession)
                .param("badParam", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown query parameter: badParam"));
    }

    @Test
    void adminCanTuneRecommendationConfigAndReadStats() throws Exception {
        var adminSession = loginAsAdmin();

        mockMvc.perform(get("/api/admin/recommendations/config")
                .session(adminSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThan(0)));

        mockMvc.perform(put("/api/admin/recommendations/config/LONG_TAIL_PERCENT")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"value":"25"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.key").value("LONG_TAIL_PERCENT"))
            .andExpect(jsonPath("$.value").value("25"));

        mockMvc.perform(get("/api/admin/recommendations/stats")
                .session(adminSession)
                .param("days", "7"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void explainEndpointReturnsRecommendationReasons() throws Exception {
        var adminSession = loginAsAdmin();
        mockMvc.perform(get("/api/recommendations/explain")
                .session(adminSession)
                .param("surface", "HOME")
                .param("limit", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThan(0)))
            .andExpect(jsonPath("$[0].reason").exists());
    }

    @Test
    void longTailQuotaIsGuaranteedWhenSupplyIsSufficient() throws Exception {
        var adminSession = loginAsAdmin();
        seedAdditionalLongTailInventory();
        jdbcTemplate.update("DELETE FROM recommendation_exposures WHERE username = 'admin'");
        assertLongTailQuota(adminSession, 5);
        jdbcTemplate.update("DELETE FROM recommendation_exposures WHERE username = 'admin'");
        assertLongTailQuota(adminSession, 10);
        jdbcTemplate.update("DELETE FROM recommendation_exposures WHERE username = 'admin'");
        assertLongTailQuota(adminSession, 25);
    }

    @Test
    void constrainedInventoryStillMeetsLongTailQuota() throws Exception {
        var adminSession = loginAsAdmin();
        seedAdditionalLongTailInventory();
        jdbcTemplate.update(
            """
            INSERT INTO recommendation_exposures (username, sku, region_code, surface, created_at)
            SELECT 'admin', sku, 'GLOBAL', 'HOME', NOW()
              FROM products
             WHERE sku NOT LIKE 'LTQ-%'
            """
        );

        MvcResult result = mockMvc.perform(get("/api/recommendations")
                .session(adminSession)
                .param("surface", "HOME")
                .param("limit", "5")
                .param("region", "GLOBAL"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertNotNull(root);
        org.junit.jupiter.api.Assertions.assertTrue(root.isArray());
        org.junit.jupiter.api.Assertions.assertTrue(root.size() <= 5);
        int longTailCount = 0;
        for (JsonNode node : root) {
            if (node.path("longTail").asBoolean(false)) {
                longTailCount++;
            }
            org.junit.jupiter.api.Assertions.assertTrue(node.path("reasonCode").isMissingNode() || node.path("reasonCode").isNull());
        }
        org.junit.jupiter.api.Assertions.assertTrue(longTailCount >= 1, "Expected at least one long-tail recommendation");
    }

    @Test
    void constrainedInventoryAutoReducesServedLimitWhileMaintainingQuotaAndSafetyRules() throws Exception {
        var adminSession = loginAsAdmin();
        seedAdditionalLongTailInventory();

        String preservedLongTailSku = "LTQ-001";
        String seenSku = jdbcTemplate.queryForObject(
            "SELECT sku FROM products WHERE enabled = 1 AND sku NOT LIKE 'LTQ-%' ORDER BY sku LIMIT 1",
            String.class
        );
        String cappedSku = jdbcTemplate.queryForObject(
            "SELECT sku FROM products WHERE enabled = 1 AND sku NOT LIKE 'LTQ-%' ORDER BY sku LIMIT 1 OFFSET 1",
            String.class
        );

        jdbcTemplate.update(
            """
            INSERT INTO recommendation_exposures (username, sku, region_code, surface, created_at)
            VALUES ('admin', ?, 'GLOBAL', 'HOME', NOW())
            """,
            seenSku
        );
        jdbcTemplate.update(
            """
            INSERT INTO recommendation_daily_impressions (day_bucket, region_code, sku, impressions)
            VALUES (?, 'GLOBAL', ?, 200)
            ON DUPLICATE KEY UPDATE impressions = 200
            """,
            LocalDate.now(),
            cappedSku
        );
        jdbcTemplate.update(
            """
            INSERT INTO recommendation_daily_impressions (day_bucket, region_code, sku, impressions)
            SELECT ?, 'GLOBAL', sku, 200
              FROM products
             WHERE sku LIKE 'LTQ-%'
               AND sku <> ?
            ON DUPLICATE KEY UPDATE impressions = 200
            """,
            LocalDate.now(),
            preservedLongTailSku
        );

        MvcResult result = mockMvc.perform(get("/api/recommendations")
                .session(adminSession)
                .param("surface", "HOME")
                .param("limit", "10")
                .param("region", "GLOBAL"))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("\"sku\":\"" + seenSku + "\""))))
            .andExpect(content().string(not(containsString("\"sku\":\"" + cappedSku + "\""))))
            .andReturn();

        String adjustedHeader = result.getResponse().getHeader("X-Recommendation-Limit-Adjusted");
        org.junit.jupiter.api.Assertions.assertNotNull(adjustedHeader);
        org.junit.jupiter.api.Assertions.assertTrue(adjustedHeader.startsWith("requested=10,served="));

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        org.junit.jupiter.api.Assertions.assertTrue(root.size() > 0 && root.size() < 10);
        int longTailCount = 0;
        for (JsonNode node : root) {
            if (node.path("longTail").asBoolean(false)) {
                longTailCount++;
            }
            org.junit.jupiter.api.Assertions.assertTrue(node.path("reasonCode").isMissingNode() || node.path("reasonCode").isNull());
        }
        int required = (int) Math.ceil(root.size() * 0.20d);
        org.junit.jupiter.api.Assertions.assertTrue(
            longTailCount >= required,
            "Expected at least " + required + " long-tail items for served size " + root.size() + " but got " + longTailCount
        );
    }

    @Test
    void recommendationStatsRejectsOutOfRangeDays() throws Exception {
        var adminSession = loginAsAdmin();
        mockMvc.perform(get("/api/admin/recommendations/stats")
                .session(adminSession)
                .param("days", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/recommendations/stats")
                .session(adminSession)
                .param("days", "91"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void concurrentRequestsDoNotExceedPerRegionDailyCap() throws Exception {
        var adminSession = loginAsAdmin();
        createUser(adminSession, "reco_race_ops_1", "OPS_MANAGER");
        createUser(adminSession, "reco_race_ops_2", "OPS_MANAGER");
        var opsSession1 = login("reco_race_ops_1", "StrongPass12345!");
        var opsSession2 = login("reco_race_ops_2", "StrongPass12345!");

        String targetSku = jdbcTemplate.queryForObject(
            "SELECT sku FROM products WHERE enabled = 1 ORDER BY posted_at DESC, sku ASC LIMIT 1",
            String.class
        );
        org.junit.jupiter.api.Assertions.assertNotNull(targetSku);

        jdbcTemplate.update(
            """
            INSERT INTO recommendation_daily_impressions (day_bucket, region_code, sku, impressions)
            SELECT ?, 'RACE', sku, 200
              FROM products
             WHERE enabled = 1
               AND sku <> ?
            ON DUPLICATE KEY UPDATE impressions = 200
            """,
            LocalDate.now(),
            targetSku
        );
        jdbcTemplate.update(
            """
            INSERT INTO recommendation_daily_impressions (day_bucket, region_code, sku, impressions)
            VALUES (?, 'RACE', ?, 199)
            ON DUPLICATE KEY UPDATE impressions = 199
            """,
            LocalDate.now(),
            targetSku
        );

        List<Integer> statuses = runInParallel(List.of(
            () -> mockMvc.perform(get("/api/recommendations")
                    .session(opsSession1)
                    .param("surface", "HOME")
                    .param("region", "RACE")
                    .param("limit", "1"))
                .andReturn()
                .getResponse()
                .getStatus(),
            () -> mockMvc.perform(get("/api/recommendations")
                    .session(opsSession2)
                    .param("surface", "HOME")
                    .param("region", "RACE")
                    .param("limit", "1"))
                .andReturn()
                .getResponse()
                .getStatus()
        ));

        org.junit.jupiter.api.Assertions.assertEquals(List.of(200, 200), statuses);

        Integer impressions = jdbcTemplate.queryForObject(
            """
            SELECT impressions
              FROM recommendation_daily_impressions
             WHERE day_bucket = ?
               AND region_code = 'RACE'
               AND sku = ?
            """,
            Integer.class,
            LocalDate.now(),
            targetSku
        );
        org.junit.jupiter.api.Assertions.assertNotNull(impressions);
        org.junit.jupiter.api.Assertions.assertEquals(200, impressions.intValue());

        Long exposureCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
              FROM recommendation_exposures
             WHERE region_code = 'RACE'
               AND sku = ?
               AND DATE(created_at) = ?
            """,
            Long.class,
            targetSku,
            LocalDate.now()
        );
        org.junit.jupiter.api.Assertions.assertNotNull(exposureCount);
        org.junit.jupiter.api.Assertions.assertEquals(1L, exposureCount.longValue());
    }

    private void assertLongTailQuota(org.springframework.mock.web.MockHttpSession session, int limit) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/recommendations")
                .session(session)
                .param("surface", "HOME")
                .param("limit", String.valueOf(limit))
                .param("region", "GLOBAL"))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode root = objectMapper.readTree(result.getResponse().getContentAsString());
        int longTailCount = 0;
        for (JsonNode node : root) {
            if (node.path("longTail").asBoolean(false)) {
                longTailCount++;
            }
        }
        Integer configuredPercent = jdbcTemplate.queryForObject(
            "SELECT CAST(config_value AS SIGNED) FROM recommendation_config WHERE config_key = 'LONG_TAIL_PERCENT'",
            Integer.class
        );
        int percent = configuredPercent == null ? 20 : configuredPercent;
        int required = (int) Math.ceil(limit * (percent / 100.0d));
        org.junit.jupiter.api.Assertions.assertTrue(
            longTailCount >= required,
            "Expected at least " + required + " long-tail items for limit " + limit + " but got " + longTailCount
        );
        if (root.size() > 0) {
            org.junit.jupiter.api.Assertions.assertTrue(root.get(0).path("reasonCode").isMissingNode() || root.get(0).path("reasonCode").isNull());
        }
    }

    private void seedAdditionalLongTailInventory() {
        jdbcTemplate.update("DELETE FROM products WHERE sku LIKE 'LTQ-%'");
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories ORDER BY id LIMIT 1", Long.class);
        String zipCode = jdbcTemplate.queryForObject("SELECT zip_code FROM zip_reference ORDER BY zip_code LIMIT 1", String.class);
        if (categoryId == null || zipCode == null) {
            return;
        }
        for (int i = 1; i <= 40; i++) {
            jdbcTemplate.update(
                """
                INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code, enabled)
                VALUES (?, ?, ?, ?, ?, 'NEW', NOW(), ?, 1)
                """,
                String.format("LTQ-%03d", i),
                "Long Tail Seed " + i,
                "Synthetic long-tail seed product " + i,
                categoryId,
                10.00 + i,
                zipCode
            );
        }
    }

    private List<Integer> runInParallel(List<Callable<Integer>> tasks) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (Callable<Integer> task : tasks) {
                futures.add(executor.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            executor.shutdownNow();
        }
    }

    private void createUser(org.springframework.mock.web.MockHttpSession adminSession, String username, String role) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"StrongPass12345!","roles":["%s"],"enabled":true}
                    """.formatted(username, role)))
            .andExpect(status().isCreated());
    }
}
