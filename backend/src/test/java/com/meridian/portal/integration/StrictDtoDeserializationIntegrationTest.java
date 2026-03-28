package com.meridian.portal.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class StrictDtoDeserializationIntegrationTest extends BaseIntegrationTest {

    @Test
    void unknownFieldsAreRejectedForAdminRecommendationAndNotificationDtos() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "strict_dto_user", "REGULAR_USER");

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"strict_dto_user_2","password":"StrongPass12345!","roles":["REGULAR_USER"],"enabled":true,"unexpected":"x"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));

        mockMvc.perform(post("/api/recommendations/events")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"eventType":"SEARCH","queryText":"scan","unknown":"x"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));

        mockMvc.perform(put("/api/notifications/preferences")
                .with(csrf())
                .session(login("strict_dto_user", "StrongPass12345!"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"dndStart":"22:00","dndEnd":"06:00","maxRemindersPerEvent":2,"unknown":"x"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));
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
