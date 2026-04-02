package com.meridian.portal.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class ApiRangeValidationIntegrationTest extends BaseIntegrationTest {

    @Test
    void tasksRejectOutOfRangePagination() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "range_regular_01", "REGULAR_USER");
        var regular = login("range_regular_01", "StrongPass12345!");

        mockMvc.perform(get("/api/tasks/my").session(regular).param("page", "-1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks/my").session(regular).param("size", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/tasks/my").session(regular).param("size", "101"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void healthAndAuditRejectOutOfRangePagination() throws Exception {
        var admin = loginAsAdmin();

        mockMvc.perform(get("/api/admin/health/alerts").session(admin).param("page", "-1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/health/alerts").session(admin).param("size", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/health/errors").session(admin).param("size", "101"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/admin/audit-logs").session(admin).param("page", "-1"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/admin/audit-logs").session(admin).param("size", "0"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void discoveryRejectsOutOfRangeLimits() throws Exception {
        var admin = loginAsAdmin();

        mockMvc.perform(get("/api/discovery/trending").session(admin).param("limit", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/trending").session(admin).param("limit", "21"))
            .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/discovery/history").session(admin).param("limit", "0"))
            .andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/discovery/history").session(admin).param("limit", "51"))
            .andExpect(status().isBadRequest());
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
