package com.meridian.portal.integration;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meridian.portal.security.SessionInactivityEnforcementFilter;
import org.junit.jupiter.api.Test;
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
class SessionTimeoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void sessionExpiresAfterConfiguredInactivityWindow() throws Exception {
        jdbcTemplate.update(
            "UPDATE users SET password_hash = ?, enabled = true, failed_attempts = 0, locked_until = NULL WHERE username = 'admin'",
            passwordEncoder.encode("AdminPass12345!")
        );
        MvcResult login = mockMvc.perform(formLogin("/login").user("admin").password("AdminPass12345!"))
            .andExpect(status().is3xxRedirection())
            .andReturn();
        MockHttpSession session = (MockHttpSession) login.getRequest().getSession(false);
        org.junit.jupiter.api.Assertions.assertNotNull(session);
        long expiredActivity = System.currentTimeMillis() - (31 * 60 * 1000L);
        session.setAttribute(SessionInactivityEnforcementFilter.LAST_ACTIVITY_ATTRIBUTE, expiredActivity);

        mockMvc.perform(get("/").session(session))
            .andExpect(status().is3xxRedirection())
            .andExpect(header().string("Location", containsString("/login?expired")));
    }
}
