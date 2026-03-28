package com.meridian.portal.unit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AuthSecurityServiceLoggingTest {

    @Test
    void bootstrapLoggingUsesRedactedSubjectNotPlainUsernameField() throws Exception {
        Path source = Path.of("src/main/java/com/meridian/portal/service/AuthSecurityService.java");
        String body = Files.readString(source);

        assertTrue(body.contains("event=auth_bootstrap_admin_created subject={}"));
        assertFalse(body.contains("event=auth_bootstrap_admin_created username={}"));
    }
}
