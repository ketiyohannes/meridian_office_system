package com.meridian.portal.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SensitiveLogSanitizationIntegrationTest extends BaseIntegrationTest {

    @Test
    void errorLoggingDoesNotPersistSecretFragmentsAcrossValidationAuthAndMalformedFlows() throws Exception {
        var admin = loginAsAdmin();
        String sentinel = "TOP_SECRET_TOKEN_918273";

        mockMvc.perform(get("/api/discovery/search")
                .session(admin)
                .param("unknownParam", sentinel))
            .andExpect(status().isBadRequest());

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"admin","title":"bad-json","description":"%s",}
                    """.formatted(sentinel)))
            .andExpect(status().isBadRequest());

        mockMvc.perform(formLogin("/login").user("admin").password("WrongPass-" + sentinel))
            .andExpect(status().is3xxRedirection());

        Long leakedCount = jdbcTemplate.queryForObject(
            """
            SELECT COUNT(*)
              FROM health_error_logs
             WHERE message LIKE CONCAT('%', ?, '%')
                OR details_json LIKE CONCAT('%', ?, '%')
            """,
            Long.class,
            sentinel,
            sentinel
        );
        assertEquals(0L, leakedCount);
    }
}
