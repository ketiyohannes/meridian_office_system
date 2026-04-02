package com.meridian.portal.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SecurityHardeningIntegrationTest extends BaseIntegrationTest {

    @Test
    void accountLocksAfterFailedAttemptsAndAllowsLoginAfterLockExpiry() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "lockout_user_01", "REGULAR_USER");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(formLogin("/login").user("lockout_user_01").password("WrongPass12345!"))
                .andExpect(status().is3xxRedirection());
        }

        LocalDateTime lockedUntilRaw = jdbcTemplate.queryForObject(
            "SELECT locked_until FROM users WHERE username = 'lockout_user_01'",
            LocalDateTime.class
        );
        Integer lockoutMinutes = jdbcTemplate.queryForObject(
            "SELECT TIMESTAMPDIFF(MINUTE, UTC_TIMESTAMP(6), locked_until) FROM users WHERE username = 'lockout_user_01'",
            Integer.class
        );
        Instant lockedUntil = lockedUntilRaw == null ? null : lockedUntilRaw.atZone(ZoneId.systemDefault()).toInstant();
        org.junit.jupiter.api.Assertions.assertNotNull(lockedUntil);
        org.junit.jupiter.api.Assertions.assertNotNull(lockoutMinutes);
        org.junit.jupiter.api.Assertions.assertTrue(lockoutMinutes >= 14 && lockoutMinutes <= 16);
        long seconds = java.time.Duration.between(Instant.now(), lockedUntil).getSeconds();
        org.junit.jupiter.api.Assertions.assertTrue(
            seconds <= 15 * 60 + 20,
            "Expected lockout window near 15 minutes, got seconds=" + seconds
        );

        mockMvc.perform(formLogin("/login").user("lockout_user_01").password("StrongPass12345!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login?locked")));

        jdbcTemplate.update(
            "UPDATE users SET locked_until = DATE_SUB(NOW(), INTERVAL 1 MINUTE), failed_attempts = 0 WHERE username = ?",
            "lockout_user_01"
        );

        mockMvc.perform(formLogin("/login").user("lockout_user_01").password("StrongPass12345!"))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/")));
    }

    @Test
    void analystCannotCreateExceptionRequest() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "analyst_blocked_01", "ANALYST");
        var analyst = login("analyst_blocked_01", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(analyst)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "requestType":"SHIFT_EXCEPTION",
                      "details":"analyst should be blocked"
                    }
                    """))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/notifications")
                .session(analyst)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/tasks/my")
                .session(analyst)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/recommendations")
                .session(analyst)
                .param("surface", "HOME")
                .param("limit", "5"))
            .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/exceptions/mine")
                .session(analyst))
            .andExpect(status().isForbidden());
    }

    @Test
    void stateChangingEndpointsRejectRequestsWithoutCsrfToken() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "csrf_user_01", "REGULAR_USER");
        var regular = login("csrf_user_01", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "requestType":"SHIFT_EXCEPTION",
                      "details":"csrf token omitted"
                    }
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void unknownJsonPropertiesAreRejectedWithBadRequest() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "payload_user_01", "REGULAR_USER");

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"payload_user_01",
                      "title":"Validate payload strictness",
                      "description":"strict JSON check",
                      "unexpectedField":"should fail"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void malformedJsonLogsAreSanitizedWithoutPayloadFragments() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "payload_user_02", "REGULAR_USER");
        String sentinel = "SENTINEL_SECRET_ABC_98765";

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"payload_user_02",
                      "title":"Malformed",
                      "description":"%s",
                    }
                    """.formatted(sentinel)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Malformed request body"));

        String details = jdbcTemplate.queryForObject(
            "SELECT details_json FROM health_error_logs WHERE request_path = '/api/tasks' ORDER BY id DESC LIMIT 1",
            String.class
        );
        org.junit.jupiter.api.Assertions.assertEquals("MALFORMED_JSON", details);
        org.junit.jupiter.api.Assertions.assertFalse(details.contains(sentinel));
    }

    @Test
    void exceptionEndpointsEnforceRoleMatrixWithoutRegression() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "ex_regular_01", "REGULAR_USER");
        createUser(admin, "ex_merch_01", "MERCHANDISER");
        createUser(admin, "ex_ops_01", "OPS_MANAGER");
        createUser(admin, "ex_analyst_01", "ANALYST");
        var regular = login("ex_regular_01", "StrongPass12345!");
        var merch = login("ex_merch_01", "StrongPass12345!");
        var ops = login("ex_ops_01", "StrongPass12345!");
        var analyst = login("ex_analyst_01", "StrongPass12345!");

        mockMvc.perform(get("/api/exceptions/mine").session(regular)).andExpect(status().isOk());
        mockMvc.perform(get("/api/exceptions/mine").session(merch)).andExpect(status().isOk());
        mockMvc.perform(get("/api/exceptions/mine").session(ops)).andExpect(status().isOk());
        mockMvc.perform(get("/api/exceptions/mine").session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/exceptions/mine").session(analyst)).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/exceptions/pending").session(ops)).andExpect(status().isOk());
        mockMvc.perform(get("/api/exceptions/pending").session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/api/exceptions/pending").session(analyst)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exceptions/pending").session(regular)).andExpect(status().isForbidden());
        mockMvc.perform(get("/api/exceptions/pending").session(merch)).andExpect(status().isForbidden());

        mockMvc.perform(get("/ops/exceptions").session(ops)).andExpect(status().isOk());
        mockMvc.perform(get("/ops/exceptions").session(admin)).andExpect(status().isOk());
        mockMvc.perform(get("/ops/exceptions").session(analyst)).andExpect(status().isForbidden());
        mockMvc.perform(get("/ops/exceptions").session(regular)).andExpect(status().isForbidden());
        mockMvc.perform(get("/ops/exceptions").session(merch)).andExpect(status().isForbidden());

        mockMvc.perform(get("/api/analytics/kpis")
                .session(analyst)
                .param("from", "2025-01-01T00:00:00")
                .param("to", "2025-01-31T23:59:59"))
            .andExpect(status().isOk());

        String payload = """
            {
              "requestType":"SHIFT_EXCEPTION",
              "details":"matrix check"
            }
            """;
        mockMvc.perform(post("/api/exceptions").with(csrf()).session(regular).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/exceptions").with(csrf()).session(merch).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/exceptions").with(csrf()).session(ops).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/exceptions").with(csrf()).session(admin).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/exceptions").with(csrf()).session(analyst).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isForbidden());

        Long exceptionForOps = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM exception_requests WHERE status = 'PENDING'",
            Long.class
        );
        mockMvc.perform(put("/api/exceptions/" + exceptionForOps + "/decision")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved": true, "comment":"ok by ops"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/exceptions").with(csrf()).session(regular).contentType(MediaType.APPLICATION_JSON).content(payload))
            .andExpect(status().isOk());
        Long exceptionForAdmin = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM exception_requests WHERE status = 'PENDING'",
            Long.class
        );
        mockMvc.perform(put("/api/exceptions/" + exceptionForAdmin + "/decision")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved": false, "comment":"no by admin"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(put("/api/exceptions/" + exceptionForAdmin + "/decision")
                .with(csrf())
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved": true, "comment":"blocked regular"}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/exceptions/" + exceptionForAdmin + "/decision")
                .with(csrf())
                .session(merch)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved": true, "comment":"blocked merch"}
                    """))
            .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/exceptions/" + exceptionForAdmin + "/decision")
                .with(csrf())
                .session(analyst)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"approved": true, "comment":"blocked analyst"}
                    """))
            .andExpect(status().isForbidden());
    }

    @Test
    void userCannotCompleteAnotherUsersTask() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "task_owner_01", "REGULAR_USER");
        createUser(admin, "task_other_01", "REGULAR_USER");
        var otherUser = login("task_other_01", "StrongPass12345!");

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"task_owner_01",
                      "title":"Owner-only task",
                      "description":"must not be completed by others"
                    }
                    """))
            .andExpect(status().isOk());

        Long taskId = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM user_tasks WHERE username = 'task_owner_01'",
            Long.class
        );

        mockMvc.perform(put("/api/tasks/" + taskId + "/complete")
                .with(csrf())
                .session(otherUser))
            .andExpect(status().isNotFound());
    }

    @Test
    void userCannotMarkAnotherUsersNotificationAsRead() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "notice_owner_01", "REGULAR_USER");
        createUser(admin, "notice_other_01", "REGULAR_USER");
        var otherUser = login("notice_other_01", "StrongPass12345!");

        jdbcTemplate.update(
            """
            INSERT INTO notifications (username, topic_code, event_key, title, body, status)
            VALUES ('notice_owner_01', 'EXCEPTION_APPROVAL_OUTCOME', 'evt-owner-1', 'Title', 'Body', 'SENT')
            """
        );
        Long notificationId = jdbcTemplate.queryForObject(
            "SELECT MAX(id) FROM notifications WHERE username = 'notice_owner_01'",
            Long.class
        );

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read")
                .with(csrf())
                .session(otherUser))
            .andExpect(status().isNotFound());
    }

    @Test
    void decidingExceptionTwiceFailsAsNonIdempotent() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "exception_user_01", "REGULAR_USER");
        createUser(admin, "exception_ops_01", "OPS_MANAGER");
        var regular = login("exception_user_01", "StrongPass12345!");
        var ops = login("exception_ops_01", "StrongPass12345!");

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(regular)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "requestType":"SHIFT_EXCEPTION",
                      "details":"first decision should work"
                    }
                    """))
            .andExpect(status().isOk());

        Long exceptionId = jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests", Long.class);

        mockMvc.perform(put("/api/exceptions/" + exceptionId + "/decision")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "approved": true,
                      "comment":"approved once"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APPROVED"));

        mockMvc.perform(put("/api/exceptions/" + exceptionId + "/decision")
                .with(csrf())
                .session(ops)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "approved": false,
                      "comment":"second decision should fail"
                    }
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Only PENDING requests can be decided"));
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
