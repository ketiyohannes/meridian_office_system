package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UatSignOffIntegrationTest extends BaseIntegrationTest {

    @Test
    void adminWorkflow_userManagementAndHealthAndRecommendationControls() throws Exception {
        var admin = loginAsAdmin();

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"uat_ops_01",
                      "password":"StrongPass12345!",
                      "roles":["OPS_MANAGER"],
                      "enabled":true
                    }
                    """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.roles", hasItem("OPS_MANAGER")));

        mockMvc.perform(get("/api/admin/health/summary").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.metricCode").value("HTTP_5XX_RATE"));

        mockMvc.perform(get("/api/admin/recommendations/config").session(admin))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThan(0)));

        mockMvc.perform(post("/api/discovery/rules")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Hide discontinued",
                      "ruleType":"EXCLUDE_SKU",
                      "targetValue":"SKU-1006",
                      "priority":50,
                      "active":true
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void merchandiserWorkflow_discoveryAndRecommendations() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "uat_merch_01", "MERCHANDISER");
        var merch = login("uat_merch_01", "StrongPass12345!");

        mockMvc.perform(get("/api/discovery/search")
                .session(merch)
                .param("keyword", "monitor")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(get("/api/recommendations")
                .session(merch)
                .param("surface", "SEARCH")
                .param("limit", "8"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        mockMvc.perform(post("/api/discovery/rules")
                .with(csrf())
                .session(merch)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "name":"Pin flagship SKU",
                      "ruleType":"PIN_SKU",
                      "targetValue":"SKU-1001",
                      "priority":1,
                      "active":true
                    }
                    """))
            .andExpect(status().isOk());
    }

    @Test
    void regularUserWorkflow_notificationsAndPreferences() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "uat_user_01", "REGULAR_USER");
        var user = login("uat_user_01", "StrongPass12345!");

        mockMvc.perform(put("/api/notifications/preferences")
                .with(csrf())
                .session(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dndStart":"21:30","dndEnd":"07:00","maxRemindersPerEvent":3}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notifications/events/approval-outcome")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"uat_user_01","eventKey":"uat-approval-1","approved":true,"details":"approved"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                .session(user)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));

        mockMvc.perform(get("/api/tasks/my")
                .session(user)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(0)));

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(user)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "requestType":"SHIFT_EXCEPTION",
                      "details":"Need late check-in exception"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    void analystWorkflow_kpiDashboardAndExports() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "uat_analyst_01", "ANALYST");
        var analyst = login("uat_analyst_01", "StrongPass12345!");

        mockMvc.perform(get("/api/analytics/kpis")
                .session(analyst)
                .param("product", "SKU-1001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.orderVolume").value(greaterThan(0)));

        mockMvc.perform(get("/api/analytics/export.csv").session(analyst))
            .andExpect(status().isOk());
    }

    @Test
    void opsManagerWorkflow_exceptionEventAndAnalyticsAccess() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "uat_ops_02", "OPS_MANAGER");
        createUser(admin, "uat_user_02", "REGULAR_USER");

        var ops = login("uat_ops_02", "StrongPass12345!");
        var regular = login("uat_user_02", "StrongPass12345!");

        mockMvc.perform(post("/api/notifications/events/approval-outcome")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"uat_user_02","eventKey":"uat-ops-approval","approved":false,"details":"rejected"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/analytics/kpis").session(ops))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "requestType":"INVENTORY_EXCEPTION",
                      "details":"Requesting override"
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/exceptions/pending").session(ops))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        Long exceptionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests", Long.class);

        mockMvc.perform(put("/api/exceptions/" + exceptionId + "/decision")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved":true,"comment":"approved by ops"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(delete("/api/discovery/history")
                .with(csrf())
                .session(regular))
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
