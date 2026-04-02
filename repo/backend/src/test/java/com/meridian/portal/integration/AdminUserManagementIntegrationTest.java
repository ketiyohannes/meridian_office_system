package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AdminUserManagementIntegrationTest extends BaseIntegrationTest {

    @Test
    void createUserAndWriteAuditLog() throws Exception {
        var session = loginAsAdmin();

        String createPayload = """
            {
              "username": "merch_01",
              "password": "StrongPass123!",
              "roles": ["MERCHANDISER"],
              "enabled": true
            }
            """;

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("merch_01"))
            .andExpect(jsonPath("$.roles", hasItem("MERCHANDISER")));

        mockMvc.perform(get("/api/admin/audit-logs?action=USER_CREATE")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThan(0)))
            .andExpect(jsonPath("$.content[0].action").value("USER_CREATE"));
    }

    @Test
    void rolePasswordAndEnabledUpdatesAreTracked() throws Exception {
        var session = loginAsAdmin();

        String createPayload = """
            {
              "username": "ops_01",
              "password": "StrongPass123!",
              "roles": ["OPS_MANAGER"],
              "enabled": true
            }
            """;

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.username").value("ops_01"));

        Long userId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'ops_01'", Long.class);

        mockMvc.perform(put("/api/admin/users/" + userId + "/roles")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"roles\":[\"OPS_MANAGER\",\"REGULAR_USER\"]}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.roles", hasItem("REGULAR_USER")));

        mockMvc.perform(put("/api/admin/users/" + userId + "/password")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"ResetPass12345!\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/admin/users/" + userId + "/enabled")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(get("/api/admin/audit-logs?actor=admin")
                .session(session))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThan(2)));
    }

    @Test
    void cannotDisableLastEnabledAdmin() throws Exception {
        var session = loginAsAdmin();

        Long adminId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'admin'", Long.class);

        mockMvc.perform(put("/api/admin/users/" + adminId + "/enabled")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"enabled\":false}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("At least one enabled ADMIN user must remain"));
    }

    @Test
    void sensitiveFieldsAreEncryptedAtRestAndMaskedInResponses() throws Exception {
        var session = loginAsAdmin();

        String createPayload = """
            {
              "username": "secure_user_01",
              "password": "StrongPass123!",
              "roles": ["REGULAR_USER"],
              "enabled": true,
              "employeeIdentifier": "EMP-12345678",
              "contactField": "5551234567"
            }
            """;

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.maskedEmployeeIdentifier").exists())
            .andExpect(jsonPath("$.maskedEmployeeIdentifier").value("********5678"))
            .andExpect(jsonPath("$.maskedContactField").value("******4567"));

        String encryptedEmployee = jdbcTemplate.queryForObject(
            "SELECT employee_identifier_encrypted FROM users WHERE username = 'secure_user_01'",
            String.class
        );
        String encryptedContact = jdbcTemplate.queryForObject(
            "SELECT contact_field_encrypted FROM users WHERE username = 'secure_user_01'",
            String.class
        );
        org.junit.jupiter.api.Assertions.assertNotNull(encryptedEmployee);
        org.junit.jupiter.api.Assertions.assertNotNull(encryptedContact);
        org.junit.jupiter.api.Assertions.assertNotEquals("EMP-12345678", encryptedEmployee);
        org.junit.jupiter.api.Assertions.assertNotEquals("5551234567", encryptedContact);
    }
}
