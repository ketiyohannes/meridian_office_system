package com.meridian.portal.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class CsrfWriteMatrixIntegrationTest extends BaseIntegrationTest {

    @Test
    void keyWriteEndpointsRequireCsrfToken() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "csrf_regular_01", "REGULAR_USER");
        createUser(admin, "csrf_merch_01", "MERCHANDISER");
        createUser(admin, "csrf_ops_01", "OPS_MANAGER");
        var regular = login("csrf_regular_01", "StrongPass12345!");
        var merch = login("csrf_merch_01", "StrongPass12345!");
        var ops = login("csrf_ops_01", "StrongPass12345!");

        Long taskId = createTaskForUser("csrf_regular_01", admin);
        Long exId = createExceptionForUser("csrf_regular_01");
        Long notificationId = seedNotification("csrf_regular_01");
        Long ruleId = createDiscoveryRule(merch);
        Long managedUserId = jdbcTemplate.queryForObject("SELECT id FROM users WHERE username = 'csrf_regular_01'", Long.class);

        // Tasks + exceptions
        mockMvc.perform(put("/api/tasks/" + taskId + "/complete").session(regular)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/tasks").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"csrf_regular_01","title":"x","description":"y"}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/exceptions").session(regular).contentType(MediaType.APPLICATION_JSON).content("""
            {"requestType":"SHIFT_EXCEPTION","details":"missing csrf"}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/exceptions/" + exId + "/decision").session(ops).contentType(MediaType.APPLICATION_JSON).content("""
            {"approved":true,"comment":"no csrf"}
            """)).andExpect(status().isForbidden());

        // Notification writes
        mockMvc.perform(put("/api/notifications/" + notificationId + "/read").session(regular)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/notifications/read-all").session(regular)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/notifications/preferences").session(regular).contentType(MediaType.APPLICATION_JSON).content("""
            {"dndStart":"21:00","dndEnd":"07:00","maxRemindersPerEvent":2}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/notifications/subscriptions").session(regular).contentType(MediaType.APPLICATION_JSON).content("""
            [{"topic":"CHECKIN_WINDOW_OPEN","subscribed":true}]
            """)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/notifications/events/checkin").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"csrf_regular_01","eventKey":"csrf-evt-1","opensAt":"2026-03-20T05:00:00Z","cutoffAt":"2026-03-20T05:30:00Z"}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/notifications/events/approval-outcome").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"csrf_regular_01","eventKey":"csrf-evt-2","approved":true,"details":"ok"}
            """)).andExpect(status().isForbidden());

        // Discovery rule writes
        mockMvc.perform(post("/api/discovery/rules").session(merch).contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"csrf","ruleType":"EXCLUDE_SKU","targetValue":"SKU-1001","priority":1,"active":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/discovery/rules/" + ruleId).session(merch).contentType(MediaType.APPLICATION_JSON).content("""
            {"name":"csrf-updated","ruleType":"EXCLUDE_SKU","targetValue":"SKU-1001","priority":2,"active":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/discovery/rules/" + ruleId).session(merch)).andExpect(status().isForbidden());

        // Admin writes
        mockMvc.perform(post("/api/admin/users").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"username":"csrf_new_admin_user","password":"StrongPass12345!","roles":["REGULAR_USER"],"enabled":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/users/" + managedUserId + "/roles").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"roles":["REGULAR_USER"]}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/users/" + managedUserId + "/password").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"password":"AnotherStrong12345!"}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/users/" + managedUserId + "/enabled").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"enabled":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/users/" + managedUserId + "/profile").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"employeeId":"EMP1234","contactPhone":"5551234"}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/notifications/templates/EXCEPTION_APPROVAL_OUTCOME").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"titleTemplate":"x","bodyTemplate":"y","active":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/notifications/definitions/EXCEPTION_APPROVAL_OUTCOME").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"reminderIntervalMinutes":20,"maxReminders":1,"actionable":true,"active":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/recommendations/config/LONG_TAIL_PERCENT").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"value":"20"}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/health/thresholds/HTTP_5XX_RATE").session(admin).contentType(MediaType.APPLICATION_JSON).content("""
            {"windowMinutes":15,"thresholdPercent":1.0,"enabled":true}
            """)).andExpect(status().isForbidden());
        mockMvc.perform(put("/api/admin/health/alerts/999/resolve").session(admin)).andExpect(status().isForbidden());
    }

    private void createUser(org.springframework.mock.web.MockHttpSession adminSession, String username, String role) throws Exception {
        mockMvc.perform(post("/api/admin/users")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","password":"StrongPass12345!","roles":["%s"],"enabled":true}
                    """.formatted(username, role)))
            .andExpect(status().isCreated());
    }

    private Long createTaskForUser(String username, org.springframework.mock.web.MockHttpSession admin) throws Exception {
        mockMvc.perform(post("/api/tasks")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"%s","title":"t1","description":"d1"}
                    """.formatted(username)))
            .andExpect(status().isOk());
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM user_tasks WHERE username = ?", Long.class, username);
    }

    private Long createExceptionForUser(String username) {
        jdbcTemplate.update(
            """
            INSERT INTO exception_requests (request_key, requester_username, request_type, details, status, created_at, updated_at)
            VALUES ('EXC-CSRF-01', ?, 'SHIFT_EXCEPTION', 'details', 'PENDING', NOW(6), NOW(6))
            """,
            username
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM exception_requests WHERE requester_username = ?", Long.class, username);
    }

    private Long seedNotification(String username) {
        jdbcTemplate.update(
            """
            INSERT INTO notifications (username, topic_code, event_key, title, body, sent_at, status)
            VALUES (?, 'CHECKIN_WINDOW_OPEN', 'n-csrf-1', 't', 'b', NOW(6), 'SENT')
            """,
            username
        );
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM notifications WHERE username = ?", Long.class, username);
    }

    private Long createDiscoveryRule(org.springframework.mock.web.MockHttpSession merch) throws Exception {
        mockMvc.perform(post("/api/discovery/rules")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf())
                .session(merch)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"name":"seed-rule","ruleType":"EXCLUDE_SKU","targetValue":"SKU-1001","priority":1,"active":true}
                    """))
            .andExpect(status().isOk());
        return jdbcTemplate.queryForObject("SELECT MAX(id) FROM discovery_rules", Long.class);
    }
}
