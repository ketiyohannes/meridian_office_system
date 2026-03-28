package com.meridian.portal.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class WorkflowTransactionalRollbackIntegrationTest extends BaseIntegrationTest {

    @Test
    void exceptionDecisionRollsBackWhenNotificationStepFails() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "rollback_req_user", "REGULAR_USER");
        createUser(admin, "rollback_ops_user", "OPS_MANAGER");
        var requester = login("rollback_req_user", "StrongPass12345!");
        var ops = login("rollback_ops_user", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(requester)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SHIFT_EXCEPTION","details":"rollback-case"}
                    """))
            .andExpect(status().isOk());
        Long exId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests WHERE requester_username='rollback_req_user'", Long.class);

        // Simulate mid-operation failure in notification step by removing required event definition.
        jdbcTemplate.update("DELETE FROM notification_event_definitions WHERE topic_code = 'EXCEPTION_APPROVAL_OUTCOME'");
        try {
            mockMvc.perform(put("/api/exceptions/" + exId + "/decision")
                    .with(csrf())
                    .session(ops)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {"approved":true,"comment":"should rollback"}
                        """))
                .andExpect(status().isNotFound());

            String finalStatus = jdbcTemplate.queryForObject("SELECT status FROM exception_requests WHERE id = ?", String.class, exId);
            assertEquals("PENDING", finalStatus);
            Long decisionAuditCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM admin_audit_logs WHERE action='EXCEPTION_REQUEST_DECIDE' AND target_id = ?",
                Long.class,
                exId
            );
            assertEquals(0L, decisionAuditCount);
        } finally {
            jdbcTemplate.update(
                """
                INSERT INTO notification_event_definitions (topic_code, reminder_interval_minutes, max_reminders, actionable, active)
                VALUES ('EXCEPTION_APPROVAL_OUTCOME', 30, 1, 1, 1)
                ON DUPLICATE KEY UPDATE
                    reminder_interval_minutes = VALUES(reminder_interval_minutes),
                    max_reminders = VALUES(max_reminders),
                    actionable = VALUES(actionable),
                    active = VALUES(active)
                """
            );
        }
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
