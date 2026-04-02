package com.meridian.portal.service;

import com.meridian.portal.config.AuthProperties;
import com.meridian.portal.domain.Role;
import com.meridian.portal.domain.RoleName;
import com.meridian.portal.domain.UserAccount;
import com.meridian.portal.repository.RoleRepository;
import com.meridian.portal.repository.UserAccountRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthSecurityService {
    private static final Logger log = LoggerFactory.getLogger(AuthSecurityService.class);

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;

    public AuthSecurityService(
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuthProperties authProperties
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
    }

    @Transactional
    public void onAuthenticationFailure(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        userAccountRepository.findByUsername(username.trim()).ifPresent(user -> {
            if (isLocked(user)) {
                return;
            }

            int attempts = user.getFailedAttempts() + 1;
            user.setFailedAttempts(attempts);
            log.warn("event=auth_login_failure subject={} failedAttempts={}", privacyId(user.getUsername()), attempts);

            if (attempts >= authProperties.getMaxFailedAttempts()) {
                user.setLockedUntil(Instant.now().plus(authProperties.getLockoutMinutes(), ChronoUnit.MINUTES));
                user.setFailedAttempts(0);
                log.warn("event=auth_lockout_applied subject={} lockoutMinutes={}", privacyId(user.getUsername()), authProperties.getLockoutMinutes());
            }

            userAccountRepository.save(user);
        });
    }

    @Transactional
    public void onAuthenticationSuccess(String username) {
        if (username == null || username.isBlank()) {
            return;
        }

        userAccountRepository.findByUsername(username.trim()).ifPresent(user -> {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
            userAccountRepository.save(user);
            log.info("event=auth_login_success subject={}", privacyId(user.getUsername()));
        });
    }

    public boolean isLocked(UserAccount user) {
        return user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now());
    }

    @Transactional
    public void bootstrapSecurityData() {
        Arrays.stream(RoleName.values()).forEach(roleName -> {
            if (roleRepository.findByName(roleName).isEmpty()) {
                Role role = new Role();
                role.setName(roleName);
                roleRepository.save(role);
            }
        });
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseThrow(() -> new IllegalStateException("ADMIN role missing during bootstrap"));

        String adminUsername = Objects.requireNonNullElse(authProperties.getBootstrapAdminUsername(), "admin").trim();
        String adminPassword = Objects.requireNonNullElse(authProperties.getBootstrapAdminPassword(), "");
        validatePassword(adminPassword);

        userAccountRepository.findByUsername(adminUsername).ifPresentOrElse(existing -> {
            if (authProperties.isBootstrapAdminResetOnStartup()) {
                existing.setPasswordHash(passwordEncoder.encode(adminPassword));
                existing.setEnabled(true);
                existing.setFailedAttempts(0);
                existing.setLockedUntil(null);
                existing.getRoles().add(adminRole);
                userAccountRepository.save(existing);
                log.warn("event=auth_bootstrap_admin_updated subject={}", privacyId(adminUsername));
            } else {
                log.info("event=auth_bootstrap_admin_exists subject={}", privacyId(adminUsername));
            }
        }, () -> {
            UserAccount admin = new UserAccount();
            admin.setUsername(adminUsername);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setEnabled(true);
            admin.setFailedAttempts(0);
            admin.setLockedUntil(null);
            admin.getRoles().add(adminRole);
            log.warn("event=auth_bootstrap_admin_created subject={}", privacyId(adminUsername));
            userAccountRepository.save(admin);
        });
    }

    private String privacyId(String username) {
        if (username == null || username.isBlank()) {
            return "anon";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(username.trim().toLowerCase().getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(12);
            for (int i = 0; i < 6; i++) {
                sb.append(String.format("%02x", bytes[i]));
            }
            return sb.toString();
        } catch (Exception ex) {
            return "hash_error";
        }
    }

    public void validatePassword(String password) {
        if (password == null || password.length() < authProperties.getMinPasswordLength()) {
            throw new IllegalArgumentException(
                "Password must be at least " + authProperties.getMinPasswordLength() + " characters"
            );
        }
    }
}
