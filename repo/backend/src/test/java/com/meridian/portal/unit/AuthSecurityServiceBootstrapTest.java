package com.meridian.portal.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.meridian.portal.config.AuthProperties;
import com.meridian.portal.domain.Role;
import com.meridian.portal.domain.RoleName;
import com.meridian.portal.domain.UserAccount;
import com.meridian.portal.repository.RoleRepository;
import com.meridian.portal.repository.UserAccountRepository;
import com.meridian.portal.service.AuthSecurityService;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthSecurityServiceBootstrapTest {

    @Test
    void existingAdminGetsUpdatedWhenResetOnStartupEnabled() {
        UserAccountRepository userRepo = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepo = Mockito.mock(RoleRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
        AuthProperties props = new AuthProperties();
        props.setBootstrapAdminUsername("admin");
        props.setBootstrapAdminPassword("NewStrongPass12345!");
        props.setBootstrapAdminResetOnStartup(true);

        UserAccount existing = new UserAccount();
        existing.setUsername("admin");
        existing.setPasswordHash("oldHash");
        existing.setEnabled(false);
        existing.setFailedAttempts(4);
        existing.setLockedUntil(Instant.now().plus(10, ChronoUnit.MINUTES));

        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(encoder.encode("NewStrongPass12345!")).thenReturn("newHash");
        when(roleRepo.findByName(any(RoleName.class))).thenAnswer(invocation -> {
            Role role = new Role();
            role.setName(invocation.getArgument(0));
            return Optional.of(role);
        });

        AuthSecurityService service = new AuthSecurityService(userRepo, roleRepo, encoder, props);
        service.bootstrapSecurityData();

        assertEquals("newHash", existing.getPasswordHash());
        assertTrue(existing.isEnabled());
        assertEquals(0, existing.getFailedAttempts());
        assertNull(existing.getLockedUntil());
        assertTrue(existing.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN));
        verify(userRepo).save(existing);
    }

    @Test
    void existingAdminIsLeftUnchangedWhenResetOnStartupDisabled() {
        UserAccountRepository userRepo = Mockito.mock(UserAccountRepository.class);
        RoleRepository roleRepo = Mockito.mock(RoleRepository.class);
        PasswordEncoder encoder = Mockito.mock(PasswordEncoder.class);
        AuthProperties props = new AuthProperties();
        props.setBootstrapAdminUsername("admin");
        props.setBootstrapAdminPassword("NewStrongPass12345!");
        props.setBootstrapAdminResetOnStartup(false);

        UserAccount existing = new UserAccount();
        existing.setUsername("admin");
        existing.setPasswordHash("oldHash");
        existing.setEnabled(false);
        existing.setFailedAttempts(3);
        Instant existingLockedUntil = Instant.now().plus(5, ChronoUnit.MINUTES);
        existing.setLockedUntil(existingLockedUntil);

        when(userRepo.findByUsername("admin")).thenReturn(Optional.of(existing));
        when(roleRepo.findByName(any(RoleName.class))).thenAnswer(invocation -> {
            Role role = new Role();
            role.setName(invocation.getArgument(0));
            return Optional.of(role);
        });

        AuthSecurityService service = new AuthSecurityService(userRepo, roleRepo, encoder, props);
        service.bootstrapSecurityData();

        assertEquals("oldHash", existing.getPasswordHash());
        assertFalse(existing.isEnabled());
        assertEquals(3, existing.getFailedAttempts());
        assertEquals(existingLockedUntil, existing.getLockedUntil());
        verify(userRepo, never()).save(existing);
        verify(encoder, never()).encode(any(String.class));
    }
}
