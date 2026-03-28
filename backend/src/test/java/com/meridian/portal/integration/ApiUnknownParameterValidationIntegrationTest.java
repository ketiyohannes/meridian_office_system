package com.meridian.portal.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApiUnknownParameterValidationIntegrationTest extends BaseIntegrationTest {

    @Test
    void unknownQueryParametersAreRejectedAcrossApiSurface() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "api_regular_01", "REGULAR_USER");
        createUser(admin, "api_ops_01", "OPS_MANAGER");
        var regular = login("api_regular_01", "StrongPass12345!");
        var ops = login("api_ops_01", "StrongPass12345!");

        // Auth
        mockMvc.perform(get("/api/auth/me").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());

        // Admin users/audit
        mockMvc.perform(get("/api/admin/users").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-logs").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());

        // Admin recommendation/notification/health
        mockMvc.perform(get("/api/admin/recommendations/config").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/recommendations/stats").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/notifications/templates").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/notifications/definitions").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/health/summary").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/health/thresholds").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/health/alerts").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/health/errors").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());

        // Analytics
        mockMvc.perform(get("/api/analytics/kpis").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/analytics/export.csv").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/analytics/export.xlsx").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/analytics/report-packages").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());

        // Discovery
        mockMvc.perform(get("/api/discovery/search").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/search/lazy").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/suggestions").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/trending").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/history").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/rules").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());

        // Recommendations
        mockMvc.perform(get("/api/recommendations").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/recommendations/explain").session(admin).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/recommendations/events")
                .with(csrf())
                .session(admin)
                .param("unknownParam", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventType":"SEARCH","queryText":"scanner"}
                    """))
            .andExpect(status().isBadRequest());

        // Notifications
        mockMvc.perform(get("/api/notifications").session(regular).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/notifications/unread-count").session(regular).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/notifications/preferences").session(regular).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/notifications/subscriptions").session(regular).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/notifications/events/checkin")
                .with(csrf())
                .session(admin)
                .param("unknownParam", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"api_regular_01","eventKey":"evt-1","opensAt":"2026-03-26T05:00:00Z","cutoffAt":"2026-03-26T05:30:00Z"}
                    """))
            .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/notifications/events/approval-outcome")
                .with(csrf())
                .session(admin)
                .param("unknownParam", "1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"api_regular_01","eventKey":"evt-2","approved":true,"details":"ok"}
                    """))
            .andExpect(status().isBadRequest());

        // Tasks / exceptions
        mockMvc.perform(get("/api/tasks/my").session(regular).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/exceptions/mine").session(regular).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/exceptions/pending").session(ops).param("unknownParam", "1"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void knownParametersStillWorkAndSecurityBehaviorIsUnchanged() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "api_regular_02", "REGULAR_USER");
        var regular = login("api_regular_02", "StrongPass12345!");

        mockMvc.perform(get("/api/discovery/search")
                .session(admin)
                .param("page", "0")
                .param("size", "5"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/kpis")
                .session(admin)
                .param("product", "SKU-1001"))
            .andExpect(status().isOk());

        // CSRF behavior remains unchanged on state-changing requests.
        mockMvc.perform(post("/api/exceptions")
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SHIFT_EXCEPTION","details":"csrf check"}
                    """))
            .andExpect(status().isForbidden());
    }

    private void createUser(org.springframework.mock.web.MockHttpSession adminSession, String username, String role) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"%s",
                      "password":"StrongPass12345!",
                      "roles":["%s"],
                      "enabled":true
                    }
                    """.formatted(username, role)))
            .andExpect(status().isCreated());
    }
}
