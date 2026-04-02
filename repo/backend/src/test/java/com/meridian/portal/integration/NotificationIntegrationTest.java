package com.meridian.portal.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meridian.portal.notification.service.NotificationService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class NotificationIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private NotificationService notificationService;

    @Test
    void unknownNotificationQueryParamIsRejected() throws Exception {
        var session = loginAsAdmin();

        mockMvc.perform(get("/api/notifications")
                .session(session)
                .param("badParam", "1"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("Unknown query parameter: badParam"));
    }

    @Test
    void invalidNotificationPaginationIsRejected() throws Exception {
        createRegularUser("assoc_invalid_page", "AssocPass12345!");
        var session = login("assoc_invalid_page", "AssocPass12345!");

        mockMvc.perform(get("/api/notifications")
                .session(session)
                .param("page", "-1")
                .param("size", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("page must be greater than or equal to 0"));

        mockMvc.perform(get("/api/notifications")
                .session(session)
                .param("page", "0")
                .param("size", "101"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("size must be between 1 and 100"));
    }

    @Test
    void userCanUpdatePreferencesAndSubscriptions() throws Exception {
        createRegularUser("assoc_01", "AssocPass12345!");
        var userSession = login("assoc_01", "AssocPass12345!");

        mockMvc.perform(put("/api/notifications/preferences")
                .with(csrf())
                .session(userSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dndStart": "22:00",
                      "dndEnd": "06:30",
                      "maxRemindersPerEvent": 2
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dndStart").value("22:00"))
            .andExpect(jsonPath("$.dndEnd").value("06:30"))
            .andExpect(jsonPath("$.maxRemindersPerEvent").value(2));

        mockMvc.perform(put("/api/notifications/subscriptions")
                .with(csrf())
                .session(userSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    [
                      {"topic":"CHECKIN_WINDOW_OPEN", "subscribed": false},
                      {"topic":"CHECKIN_CUTOFF_WARNING", "subscribed": true}
                    ]
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].topic", hasItem("CHECKIN_WINDOW_OPEN")));
    }

    @Test
    void approvalOutcomeCanBeDeliveredAndMarkedRead() throws Exception {
        createRegularUser("assoc_02", "AssocPass12345!");
        var adminSession = loginAsAdmin();
        var userSession = login("assoc_02", "AssocPass12345!");

        mockMvc.perform(put("/api/notifications/preferences")
                .with(csrf())
                .session(userSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "dndStart": "00:00",
                      "dndEnd": "00:00",
                      "maxRemindersPerEvent": 3
                    }
                    """))
            .andExpect(status().isOk());

        mockMvc.perform(post("/api/notifications/events/approval-outcome")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"assoc_02",
                      "eventKey":"approval-evt-1",
                      "approved": true,
                      "details":"Policy override accepted"
                    }
                    """))
            .andExpect(status().isOk());

        notificationService.processDueEvents();

        mockMvc.perform(get("/api/notifications")
                .session(userSession)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.content[0].topic").value("EXCEPTION_APPROVAL_OUTCOME"))
            .andExpect(jsonPath("$.content[0].status").value("SENT"));

        mockMvc.perform(get("/api/notifications/unread-count")
                .session(userSession))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value(greaterThanOrEqualTo(1)));

        Long notificationId = jdbcTemplate.queryForObject(
            "SELECT id FROM notifications WHERE username = ? ORDER BY sent_at DESC LIMIT 1",
            Long.class,
            "assoc_02"
        );

        mockMvc.perform(put("/api/notifications/" + notificationId + "/read")
                .with(csrf())
                .session(userSession))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/notifications")
                .session(userSession)
                .param("page", "0")
                .param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("READ"));

        mockMvc.perform(put("/api/notifications/read-all")
                .with(csrf())
                .session(userSession))
            .andExpect(status().isOk());
    }

    @Test
    void checkinEventsScheduleCutoffReminderAndRespectMaxReminderValidation() throws Exception {
        createRegularUser("assoc_03", "AssocPass12345!");
        var adminSession = loginAsAdmin();

        Instant now = Instant.now();
        Instant opensAt = now.minusSeconds(30);
        Instant cutoffAt = now.plusSeconds(15 * 60L);

        mockMvc.perform(post("/api/notifications/events/checkin")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username":"assoc_03",
                      "eventKey":"checkin-evt-1",
                      "opensAt":"%s",
                      "cutoffAt":"%s"
                    }
                    """.formatted(opensAt.toString(), cutoffAt.toString())))
            .andExpect(status().isOk());

        Integer secondsGap = jdbcTemplate.queryForObject(
            """
            SELECT TIMESTAMPDIFF(SECOND, w.event_time, c.event_time)
            FROM notification_events w
            JOIN notification_events c
              ON w.username = c.username
             AND w.event_key = c.event_key
            WHERE w.username = ?
              AND w.topic_code = 'CHECKIN_CUTOFF_WARNING'
              AND c.topic_code = 'CHECKIN_MISSED'
            LIMIT 1
            """,
            Integer.class,
            "assoc_03"
        );

        org.junit.jupiter.api.Assertions.assertNotNull(secondsGap);
        org.junit.jupiter.api.Assertions.assertEquals(960, secondsGap.intValue());

        mockMvc.perform(put("/api/admin/notifications/definitions/CHECKIN_WINDOW_OPEN")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminderIntervalMinutes": 15,
                      "maxReminders": 4,
                      "actionable": false,
                      "active": true
                    }
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    void adminCanUpdateNotificationDefinitionsAndTemplatesWithAuditTrail() throws Exception {
        var adminSession = loginAsAdmin();

        mockMvc.perform(put("/api/admin/notifications/templates/EXCEPTION_APPROVAL_OUTCOME")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "titleTemplate": "Approval status changed",
                      "bodyTemplate": "Outcome: {{outcome}}",
                      "active": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.topic").value("EXCEPTION_APPROVAL_OUTCOME"))
            .andExpect(jsonPath("$.titleTemplate").value("Approval status changed"));

        mockMvc.perform(put("/api/admin/notifications/definitions/EXCEPTION_APPROVAL_OUTCOME")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "reminderIntervalMinutes": 20,
                      "maxReminders": 1,
                      "actionable": true,
                      "active": true
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.reminderIntervalMinutes").value(20));

        mockMvc.perform(get("/api/admin/audit-logs")
                .session(adminSession)
                .param("action", "NOTIFICATION_TEMPLATE_UPDATE"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(greaterThanOrEqualTo(1)));
    }

    private void createRegularUser(String username, String password) throws Exception {
        var adminSession = loginAsAdmin();
        mockMvc.perform(post("/api/admin/users")
                .with(csrf())
                .session(adminSession)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "%s",
                      "password": "%s",
                      "roles": ["REGULAR_USER"],
                      "enabled": true
                    }
                    """.formatted(username, password)))
            .andExpect(status().isCreated());
    }
}
