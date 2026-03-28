package com.meridian.portal.config;

import com.meridian.portal.exception.ValidationException;
import java.util.Arrays;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class ProductionHardeningValidator {

    private static final String DEFAULT_BOOTSTRAP_PASSWORD = "ChangeMe12345!";
    private static final String DEFAULT_ENCRYPTION_KEY = "0123456789ABCDEF0123456789ABCDEF";

    public ProductionHardeningValidator(
        Environment environment,
        AuthProperties authProperties,
        DataSecurityProperties dataSecurityProperties
    ) {
        if (!isProdProfile(environment)) {
            return;
        }

        String bootstrapPassword = authProperties.getBootstrapAdminPassword();
        String encryptionKey = dataSecurityProperties.getDataEncryptionKey();

        List<String> violations = new java.util.ArrayList<>();
        if (bootstrapPassword == null || bootstrapPassword.length() < 12) {
            violations.add("MERIDIAN_BOOTSTRAP_ADMIN_PASSWORD must be set to a strong value (min 12 chars)");
        }
        if (DEFAULT_BOOTSTRAP_PASSWORD.equals(bootstrapPassword)) {
            violations.add("MERIDIAN_BOOTSTRAP_ADMIN_PASSWORD cannot use the default value");
        }
        if (encryptionKey == null || encryptionKey.length() != 32) {
            violations.add("MERIDIAN_DATA_ENCRYPTION_KEY must be exactly 32 characters");
        }
        if (DEFAULT_ENCRYPTION_KEY.equals(encryptionKey)) {
            violations.add("MERIDIAN_DATA_ENCRYPTION_KEY cannot use the default value");
        }

        if (!violations.isEmpty()) {
            throw new ValidationException("Production hardening validation failed: " + String.join("; ", violations));
        }
    }

    private boolean isProdProfile(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}
