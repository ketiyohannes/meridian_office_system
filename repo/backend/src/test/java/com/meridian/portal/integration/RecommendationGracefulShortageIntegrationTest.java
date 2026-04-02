package com.meridian.portal.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class RecommendationGracefulShortageIntegrationTest extends BaseIntegrationTest {

    @Test
    void recommendationsReturn200AndNoShortageReasonWhenInventoryCannotSatisfyRequestedLimit() throws Exception {
        var admin = loginAsAdmin();
        jdbcTemplate.update(
            """
            INSERT INTO recommendation_exposures (username, sku, region_code, surface, created_at)
            SELECT 'admin', sku, 'GLOBAL', 'HOME', NOW()
              FROM products
             WHERE enabled = 1
            """
        );

        mockMvc.perform(get("/api/recommendations")
                .session(admin)
                .param("surface", "HOME")
                .param("limit", "10")
                .param("region", "GLOBAL"))
            .andExpect(status().isOk())
            .andExpect(header().doesNotExist("X-Recommendation-Reason-Code"))
            .andExpect(header().string("X-Recommendation-Limit-Adjusted", "requested=10,served=0"))
            .andExpect(jsonPath("$.length()").value(0));
    }
}
