package com.meridian.portal.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meridian.portal.health.service.HealthMonitoringService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class HealthLifecycleAndIsolationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private HealthMonitoringService healthMonitoringService;

    @Test
    void healthAlertLifecycleCreateListResolveAndPreventDoubleResolve() throws Exception {
        var admin = loginAsAdmin();

        mockMvc.perform(put("/api/admin/health/thresholds/HTTP_5XX_RATE")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"windowMinutes":15,"thresholdPercent":1.0,"enabled":true}
                    """))
            .andExpect(status().isOk());

        Instant now = Instant.now();
        for (int i = 0; i < 20; i++) {
            healthMonitoringService.recordHttpStatus(500, "/api/test/error", now.plusSeconds(i));
        }
        for (int i = 0; i < 5; i++) {
            healthMonitoringService.recordHttpStatus(200, "/api/test/ok", now.plusSeconds(30 + i));
        }

        mockMvc.perform(get("/api/admin/health/alerts")
                .session(admin)
                .param("resolved", "false")
                .param("page", "0")
                .param("size", "20"))
            .andExpect(status().isOk());

        Long alertId = jdbcTemplate.queryForObject(
            "SELECT id FROM health_alerts WHERE resolved = 0 ORDER BY created_at DESC LIMIT 1",
            Long.class
        );
        assertTrue(alertId != null && alertId > 0);

        mockMvc.perform(put("/api/admin/health/alerts/" + alertId + "/resolve")
                .with(csrf())
                .session(admin))
            .andExpect(status().isOk());

        Integer resolved = jdbcTemplate.queryForObject("SELECT resolved FROM health_alerts WHERE id = ?", Integer.class, alertId);
        assertEquals(1, resolved);

        mockMvc.perform(put("/api/admin/health/alerts/" + alertId + "/resolve")
                .with(csrf())
                .session(admin))
            .andExpect(status().isNotFound());
    }

    @Test
    void crossUserIsolationHoldsForTasksNotificationsAndExceptions() throws Exception {
        var admin = loginAsAdmin();
        createUser(admin, "iso_user_a", "REGULAR_USER");
        createUser(admin, "iso_user_b", "REGULAR_USER");
        var userA = login("iso_user_a", "StrongPass12345!");
        var userB = login("iso_user_b", "StrongPass12345!");

        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"iso_user_a","title":"Task A1","description":"only A"}
                    """))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/tasks")
                .with(csrf())
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"username":"iso_user_b","title":"Task B1","description":"only B"}
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(userA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SHIFT_EXCEPTION","details":"A exception"}
                    """))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/exceptions")
                .with(csrf())
                .session(userB)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"requestType":"SHIFT_EXCEPTION","details":"B exception"}
                    """))
            .andExpect(status().isOk());

        jdbcTemplate.update(
            """
            INSERT INTO notifications (username, topic_code, event_key, title, body, sent_at, status)
            VALUES ('iso_user_a', 'CHECKIN_WINDOW_OPEN', 'iso-a-1', 't', 'b', NOW(6), 'SENT')
            """
        );
        jdbcTemplate.update(
            """
            INSERT INTO notifications (username, topic_code, event_key, title, body, sent_at, status)
            VALUES ('iso_user_b', 'CHECKIN_WINDOW_OPEN', 'iso-b-1', 't', 'b', NOW(6), 'SENT')
            """
        );

        mockMvc.perform(get("/api/tasks/my").session(userA).param("page", "0").param("size", "20"))
            .andExpect(status().isOk());
        mockMvc.perform(get("/api/tasks/my").session(userB).param("page", "0").param("size", "20"))
            .andExpect(status().isOk());
        Long tasksA = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_tasks WHERE username='iso_user_a'", Long.class);
        Long tasksB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM user_tasks WHERE username='iso_user_b'", Long.class);
        assertEquals(1L, tasksA);
        assertEquals(1L, tasksB);

        mockMvc.perform(put("/api/notifications/read-all")
                .with(csrf())
                .session(userA))
            .andExpect(status().isOk());

        Long readA = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notifications WHERE username='iso_user_a' AND status='READ'", Long.class);
        Long readB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM notifications WHERE username='iso_user_b' AND status='READ'", Long.class);
        assertEquals(1L, readA);
        assertEquals(0L, readB);

        Long exA = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exception_requests WHERE requester_username='iso_user_a'", Long.class);
        Long exB = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM exception_requests WHERE requester_username='iso_user_b'", Long.class);
        assertEquals(1L, exA);
        assertEquals(1L, exB);
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
