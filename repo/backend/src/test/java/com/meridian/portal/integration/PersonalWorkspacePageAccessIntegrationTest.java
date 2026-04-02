package com.meridian.portal.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class PersonalWorkspacePageAccessIntegrationTest extends BaseIntegrationTest {

    @Test
    void analystIsForbiddenFromNotificationsAndTasksPages() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "page_analyst_01", "ANALYST");
        var analyst = login("page_analyst_01", "StrongPass12345!");

        mockMvc.perform(get("/notifications").session(analyst))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/tasks").session(analyst))
            .andExpect(status().isForbidden());
    }

    @Test
    void allowedRolesCanAccessNotificationsAndTasksPages() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "page_regular_01", "REGULAR_USER");
        createUser(admin, "page_merch_01", "MERCHANDISER");
        createUser(admin, "page_ops_01", "OPS_MANAGER");

        var regular = login("page_regular_01", "StrongPass12345!");
        var merch = login("page_merch_01", "StrongPass12345!");
        var ops = login("page_ops_01", "StrongPass12345!");

        mockMvc.perform(get("/notifications").session(regular)).andExpect(status().isOk());
        mockMvc.perform(get("/notifications").session(merch)).andExpect(status().isOk());
        mockMvc.perform(get("/notifications").session(ops)).andExpect(status().isOk());
        mockMvc.perform(get("/notifications").session(admin)).andExpect(status().isOk());

        mockMvc.perform(get("/tasks").session(regular)).andExpect(status().isOk());
        mockMvc.perform(get("/tasks").session(merch)).andExpect(status().isOk());
        mockMvc.perform(get("/tasks").session(ops)).andExpect(status().isOk());
        mockMvc.perform(get("/tasks").session(admin)).andExpect(status().isOk());
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
