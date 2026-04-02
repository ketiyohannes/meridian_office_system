package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class AuditLogIntegrationTest extends BaseIntegrationTest {

    @Test
    void auditLogSupportsFilteringAndPagination() throws Exception {
        var session = loginAsAdmin();

        String createPayload = """
            {
              "username": "store_assoc_01",
              "password": "StrongPass123!",
              "roles": ["REGULAR_USER"],
              "enabled": true
            }
            """;

        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(createPayload))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/api/admin/audit-logs")
                .session(session)
                .param("action", "USER_CREATE")
                .param("targetType", "USER")
                .param("targetUsername", "store_assoc")
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0))
            .andExpect(jsonPath("$.size").value(10))
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void auditLogInvalidPaginationIsRejected() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/admin/audit-logs")
                .session(session)
                .param("page", "-1")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"));

        mockMvc.perform(get("/api/admin/audit-logs")
                .session(session)
                .param("page", "0")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }
}
