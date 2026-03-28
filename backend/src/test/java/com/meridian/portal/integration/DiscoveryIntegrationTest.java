package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;

class DiscoveryIntegrationTest extends BaseIntegrationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void discoverySearchReturnsPagedResults() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)));
    }

    @Test
    void discoveryLazySearchSupportsCursorPagination() throws Exception {
        var session = loginAsAdmin();

        var first = mockMvc.perform(get("/api/discovery/search/lazy")
                .session(session)
                .param("size", "5"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.hasMore").exists())
            .andReturn();

        JsonNode firstBody = objectMapper.readTree(first.getResponse().getContentAsString());
        if (firstBody.path("hasMore").asBoolean(false)) {
            String cursor = firstBody.path("nextCursor").asText();
            mockMvc.perform(get("/api/discovery/search/lazy")
                    .session(session)
                    .param("size", "5")
                    .param("cursor", cursor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
        }
    }

    @Test
    void discoveryLazySearchRejectsInvalidCursorFormat() throws Exception {
        var session = loginAsAdmin();
        mockMvc.perform(get("/api/discovery/search/lazy")
                .session(session)
                .param("cursor", "bad-cursor")
                .param("size", "5"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Invalid cursor format"));
    }

    @Test
    void unknownSearchParamIsRejected() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("badParam", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown query parameter: badParam"));
    }

    @Test
    void invalidSearchPaginationIsRejected() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("page", "-1")
                .param("size", "5"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"));

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("page", "0")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }

    @Test
    void zipDistanceValidationEdgeCasesAreHandled() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("zipCode", "10001")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Both zipCode and distanceMiles are required when using distance filtering"));

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("distanceMiles", "25")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Both zipCode and distanceMiles are required when using distance filtering"));

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("zipCode", "99999")
                .param("distanceMiles", "25")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown ZIP code: 99999"));
    }

    @Test
    void zipDistanceFilterIncludesNearbyAndExcludesFarProducts() throws Exception {
        var session = loginAsAdmin();

        var zipPair = jdbcTemplate.queryForMap(
            """
            SELECT z1.zip_code AS near_zip, z2.zip_code AS far_zip
              FROM zip_reference z1
              JOIN zip_reference z2 ON z1.zip_code <> z2.zip_code
             ORDER BY (ABS(z1.latitude - z2.latitude) + ABS(z1.longitude - z2.longitude)) DESC
             LIMIT 1
            """
        );
        String nearZip = String.valueOf(zipPair.get("near_zip"));
        String farZip = String.valueOf(zipPair.get("far_zip"));
        Long categoryId = jdbcTemplate.queryForObject("SELECT id FROM categories ORDER BY id LIMIT 1", Long.class);

        jdbcTemplate.update("DELETE FROM products WHERE sku IN ('ZIPTEST-NEAR','ZIPTEST-FAR')");
        jdbcTemplate.update(
            """
            INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code, enabled)
            VALUES ('ZIPTEST-NEAR', 'ZIP Near Product', 'near zip test', ?, 11.00, 'NEW', NOW(), ?, 1)
            """,
            categoryId,
            nearZip
        );
        jdbcTemplate.update(
            """
            INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code, enabled)
            VALUES ('ZIPTEST-FAR', 'ZIP Far Product', 'far zip test', ?, 12.00, 'NEW', NOW(), ?, 1)
            """,
            categoryId,
            farZip
        );

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("keyword", "ZIPTEST-")
                .param("zipCode", nearZip)
                .param("distanceMiles", "5")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.sku=='ZIPTEST-NEAR')]").isNotEmpty())
            .andExpect(jsonPath("$.content[?(@.sku=='ZIPTEST-FAR')]").isEmpty());
    }

    @Test
    void suggestionsRequireAtLeastTwoCharacters() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/suggestions")
                .session(session)
                .param("q", "E"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/discovery/suggestions")
                .session(session)
                .param("q", "EL"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    void trendingAndHistoryLifecycleWorks() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/search")
                .session(session)
                .param("keyword", "monitor")
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/discovery/history")
                .session(session)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasItem("monitor")));

        mockMvc.perform(get("/api/discovery/trending")
                .session(session)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThan(0)));

        mockMvc.perform(delete("/api/discovery/history")
                .with(csrf())
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.deletedCount").value(greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/discovery/history")
                .session(session)
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void merchandiserCanManageRulesAndExcludeSkuAffectsSearch() throws Exception {
        var admin = loginAsAdmin();
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"rule_merch",
                      "password":"StrongPass12345!",
                      "roles":["MERCHANDISER"],
                      "enabled":true
                    }
                    """))
            .andExpect(status().isCreated());

        var merch = login("rule_merch", "StrongPass12345!");

        mockMvc.perform(post("/api/discovery/rules")
                .with(csrf())
                .session(merch)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Exclude SKU-1001",
                      "ruleType":"EXCLUDE_SKU",
                      "targetValue":"SKU-1001",
                      "priority":10,
                      "active":true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.ruleType").value("EXCLUDE_SKU"));

        mockMvc.perform(get("/api/discovery/rules").session(merch))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/discovery/search")
                .session(merch)
                .param("keyword", "wireless")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[?(@.sku=='SKU-1001')]").isEmpty());
    }
}
