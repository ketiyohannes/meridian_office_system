package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class OperationsWorkflowIntegrationTest extends BaseIntegrationTest {

    @Test
    void taskAssignmentAndCompletionFlowWorks() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "ops_task_user", "REGULAR_USER");
        var regular = login("ops_task_user", "StrongPass12345!");

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"ops_task_user",
                      "title":"Cycle count aisle 4",
                      "description":"Complete before noon"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("OPEN"));

        mockMvc.perform(get("/api/tasks/my")
                .session(regular)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));

        Long taskId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM user_tasks WHERE username = 'ops_task_user'", Long.class);
        mockMvc.perform(put("/api/tasks/" + taskId + "/complete")
                .with(csrf())
                .session(regular))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void exceptionRequestApprovalFlowWorks() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "ops_exception_user", "REGULAR_USER");
        createUser(admin, "ops_exception_mgr", "OPS_MANAGER");
        var regular = login("ops_exception_user", "StrongPass12345!");
        var ops = login("ops_exception_mgr", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "requestType":"SHIFT_EXCEPTION",
                      "details":"Need grace period today"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PENDING"));

        mockMvc.perform(get("/api/exceptions/pending").session(ops))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()", greaterThanOrEqualTo(1)));

        Long exceptionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests", Long.class);
        mockMvc.perform(put("/api/exceptions/" + exceptionId + "/decision")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "approved":false,
                      "comment":"Not approved"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJECTED"));

        mockMvc.perform(get("/api/exceptions/mine")
                .session(regular))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].status").value("REJECTED"));
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
