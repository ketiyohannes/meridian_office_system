package com.meridian.portal.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AnalystOperationsViewIntegrationTest extends BaseIntegrationTest {

    @Test
    void analystCannotAccessOpsExceptionsViewOrPendingApiButOpsCanDecide() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "ops_analyst_01", "ANALYST");
        createUser(admin, "ops_regular_01", "REGULAR_USER");
        createUser(admin, "ops_manager_01", "OPS_MANAGER");
        var analyst = login("ops_analyst_01", "StrongPass12345!");
        var regular = login("ops_regular_01", "StrongPass12345!");
        var ops = login("ops_manager_01", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SHIFT_EXCEPTION","details":"needs review"}
                    """))
            .andExpect(status().isOk());
        Long id = jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests", Long.class);

        mockMvc.perform(get("/ops/exceptions").session(analyst))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/exceptions/pending").session(analyst))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/exceptions/" + id + "/decision")
                .with(csrf())
                .session(analyst)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved":true,"comment":"analyst should not decide"}
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/exceptions/" + id + "/decision")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved":true,"comment":"ops can decide"}
                    """))
            .andExpect(status().isOk());
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
