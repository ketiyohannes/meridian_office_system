package com.meridian.portal.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class OperationsViewIntegrationTest extends BaseIntegrationTest {

    @Test
    void operationsViewUnifiesKpisAndWorkflowPanelsWithRoleAwareSections() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "ops_view_admin_01", "ADMIN");
        createUser(admin, "ops_view_ops_01", "OPS_MANAGER");
        createUser(admin, "ops_view_analyst_01", "ANALYST");
        createUser(admin, "ops_view_regular_01", "REGULAR_USER");

        var adminSession = login("ops_view_admin_01", "StrongPass12345!");
        var opsSession = login("ops_view_ops_01", "StrongPass12345!");
        var analystSession = login("ops_view_analyst_01", "StrongPass12345!");
        var regularSession = login("ops_view_regular_01", "StrongPass12345!");

        mockMvc.perform(get("/operations").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Unified Operations View")))
            .andExpect(content().string(containsString("KPI Filters & Exports")))
            .andExpect(content().string(containsString("Exception Queue")))
            .andExpect(content().string(containsString("Task Workflow")))
            .andExpect(content().string(containsString("Cancellation Reasons")))
            .andExpect(content().string(containsString("Nightly Report Packages")));

        mockMvc.perform(get("/operations").session(opsSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Exception Queue")))
            .andExpect(content().string(containsString("Task Workflow")))
            .andExpect(content().string(containsString("/js/operations.js")));

        mockMvc.perform(get("/operations").session(analystSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Exception approvals are restricted")))
            .andExpect(content().string(not(containsString("Open full exception workflow"))));

        mockMvc.perform(get("/operations").session(regularSession))
            .andExpect(status().isForbidden());
    }

    @Test
    void homeNavigationUsesUnifiedOperationsEntryForOpsRoles() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "ops_home_admin_01", "ADMIN");
        createUser(admin, "ops_home_ops_01", "OPS_MANAGER");
        createUser(admin, "ops_home_analyst_01", "ANALYST");

        var adminSession = login("ops_home_admin_01", "StrongPass12345!");
        var opsSession = login("ops_home_ops_01", "StrongPass12345!");
        var analystSession = login("ops_home_analyst_01", "StrongPass12345!");

        mockMvc.perform(get("/").session(adminSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Open Unified Operations View")))
            .andExpect(content().string(not(containsString("Open KPI Dashboard"))))
            .andExpect(content().string(not(containsString("Review Exception Requests"))));

        mockMvc.perform(get("/").session(opsSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Open Unified Operations View")))
            .andExpect(content().string(not(containsString("Open KPI Dashboard"))))
            .andExpect(content().string(not(containsString("Review Exception Requests"))));

        mockMvc.perform(get("/").session(analystSession))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("Open Unified Operations View")))
            .andExpect(content().string(not(containsString("Open KPI Dashboard"))))
            .andExpect(content().string(not(containsString("Review Exception Requests"))));
    }

    private void createUser(org.springframework.mock.web.MockHttpSession adminSession, String username, String role) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(adminSession)
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"StrongPass12345!","roles":["%s"],"enabled":true}
                    """.formatted(username, role)))
            .andExpect(status().isCreated());
    }
}
