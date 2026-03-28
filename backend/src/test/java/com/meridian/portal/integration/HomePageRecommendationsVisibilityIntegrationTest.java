package com.meridian.portal.integration;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class HomePageRecommendationsVisibilityIntegrationTest extends BaseIntegrationTest {

    @Test
    void entitledRolesSeeRecommendationsFetcherOnHome() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "home_merch_01", "MERCHANDISER");
        var merch = login("home_merch_01", "StrongPass12345!");

        mockMvc.perform(get("/").session(merch))
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("/api/recommendations?surface=HOME&limit=6")));
    }

    @Test
    void nonEntitledRolesDoNotRenderRecommendationsFetcherOnHome() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "home_analyst_01", "ANALYST");
        createUser(admin, "home_regular_01", "REGULAR_USER");
        var analyst = login("home_analyst_01", "StrongPass12345!");
        var regular = login("home_regular_01", "StrongPass12345!");

        mockMvc.perform(get("/").session(analyst))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("/api/recommendations?surface=HOME&limit=6"))))
            .andExpect(content().string(containsString("Recommendations are available for Merchandisers, Ops Managers, and Administrators.")))
            .andExpect(content().string(not(containsString("Open Notification Inbox"))))
            .andExpect(content().string(not(containsString("Open Personal Tasks"))));

        mockMvc.perform(get("/").session(regular))
            .andExpect(status().isOk())
            .andExpect(content().string(not(containsString("/api/recommendations?surface=HOME&limit=6"))))
            .andExpect(content().string(containsString("Recommendations are available for Merchandisers, Ops Managers, and Administrators.")))
            .andExpect(content().string(containsString("Open Notification Inbox")))
            .andExpect(content().string(containsString("Open Personal Tasks")));
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
