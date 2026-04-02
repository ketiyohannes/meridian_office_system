package com.meridian.portal.integration;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    void loginPageIsPublic() throws Exception {
        mockMvc.perform(get("/login"))
            .andExpect(status().isOk());
    }

    @Test
    void adminPageRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/admin/users"))
            .andExpect(status().is3xxRedirection());
    }

    @Test
    void adminCanAuthenticateAndReadCurrentUser() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin"))
            .andExpect(jsonPath("$.roles", hasItem("ROLE_ADMIN")));
    }
}
