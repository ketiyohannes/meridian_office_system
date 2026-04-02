package com.meridian.portal.integration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void resetMutableData() {
        jdbcTemplate.update("DELETE FROM recommendation_daily_impressions");
        jdbcTemplate.update("DELETE FROM recommendation_exposures");
        jdbcTemplate.update("DELETE FROM recommendation_events");
        jdbcTemplate.update("DELETE FROM notifications");
        jdbcTemplate.update("DELETE FROM notification_events");
        jdbcTemplate.update("DELETE FROM notification_subscriptions");
        jdbcTemplate.update("DELETE FROM user_notification_preferences");
        jdbcTemplate.update("DELETE FROM user_tasks");
        jdbcTemplate.update("DELETE FROM exception_requests");
        jdbcTemplate.update("DELETE FROM discovery_rules");
        jdbcTemplate.update("DELETE FROM search_query_events");
        jdbcTemplate.update("DELETE FROM admin_audit_logs");
        jdbcTemplate.update("DELETE FROM user_roles WHERE user_id IN (SELECT id FROM users WHERE username <> 'admin')");
        jdbcTemplate.update("DELETE FROM users WHERE username <> 'admin'");
        jdbcTemplate.update(
            "UPDATE users SET password_hash = ?, enabled = true, failed_attempts = 0, locked_until = NULL WHERE username = 'admin'",
            passwordEncoder.encode("AdminPass12345!")
        );
    }

    protected MockHttpSession loginAsAdmin() throws Exception {
        return login("admin", "AdminPass12345!");
    }

    protected MockHttpSession login(String username, String password) throws Exception {
        MvcResult login = mockMvc.perform(formLogin("/login").user(username).password(password))
            .andReturn();
        return (MockHttpSession) login.getRequest().getSession(false);
    }
}
