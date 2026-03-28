package com.meridian.portal.service;

import com.meridian.portal.domain.Role;
import com.meridian.portal.domain.RoleName;
import com.meridian.portal.domain.UserAccount;
import com.meridian.portal.dto.AdminUserResponse;
import com.meridian.portal.dto.CreateUserRequest;
import com.meridian.portal.dto.ResetPasswordRequest;
import com.meridian.portal.dto.UpdateEnabledRequest;
import com.meridian.portal.dto.UpdateUserProfileRequest;
import com.meridian.portal.dto.UpdateRolesRequest;
import com.meridian.portal.exception.ConflictException;
import com.meridian.portal.exception.NotFoundException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.repository.RoleRepository;
import com.meridian.portal.repository.UserAccountRepository;
import com.meridian.portal.security.SensitiveDataCryptoService;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserManagementService {

    private final UserAccountRepository userAccountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthSecurityService authSecurityService;
    private final AdminAuditLogService adminAuditLogService;
    private final SensitiveDataCryptoService sensitiveDataCryptoService;

    public AdminUserManagementService(
        UserAccountRepository userAccountRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        AuthSecurityService authSecurityService,
        AdminAuditLogService adminAuditLogService,
        SensitiveDataCryptoService sensitiveDataCryptoService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authSecurityService = authSecurityService;
        this.adminAuditLogService = adminAuditLogService;
        this.sensitiveDataCryptoService = sensitiveDataCryptoService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> listUsers() {
        return userAccountRepository.findAll().stream()
            .sorted(Comparator.comparing(UserAccount::getUsername, String.CASE_INSENSITIVE_ORDER))
            .map(this::toResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public AdminUserResponse getUser(Long id) {
        UserAccount user = findUser(id);
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse createUser(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        if (userAccountRepository.existsByUsername(username)) {
            throw new ConflictException("Username already exists");
        }

        authSecurityService.validatePassword(request.password());

        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled());
        user.setEmployeeIdentifierEncrypted(sensitiveDataCryptoService.encrypt(request.employeeIdentifier()));
        user.setContactFieldEncrypted(sensitiveDataCryptoService.encrypt(request.contactField()));
        user.getRoles().clear();
        user.getRoles().addAll(resolveRoles(request.roles()));

        UserAccount created = userAccountRepository.save(user);
        adminAuditLogService.log(
            "USER_CREATE",
            "USER",
            created.getId(),
            created.getUsername(),
            "roles=" + formatRoles(created.getRoles()) + ",enabled=" + created.isEnabled()
        );
        return toResponse(created);
    }

    @Transactional
    public AdminUserResponse updateRoles(Long userId, UpdateRolesRequest request) {
        UserAccount user = findUser(userId);
        Set<Role> newRoles = resolveRoles(request.roles());

        enforceAtLeastOneEnabledAdmin(user, newRoles, user.isEnabled());

        user.getRoles().clear();
        user.getRoles().addAll(newRoles);
        UserAccount updated = userAccountRepository.save(user);
        adminAuditLogService.log(
            "USER_ROLES_UPDATE",
            "USER",
            updated.getId(),
            updated.getUsername(),
            "roles=" + formatRoles(updated.getRoles())
        );
        return toResponse(updated);
    }

    @Transactional
    public AdminUserResponse updateProfile(Long userId, UpdateUserProfileRequest request) {
        UserAccount user = findUser(userId);
        user.setEmployeeIdentifierEncrypted(sensitiveDataCryptoService.encrypt(request.employeeIdentifier()));
        user.setContactFieldEncrypted(sensitiveDataCryptoService.encrypt(request.contactField()));
        UserAccount updated = userAccountRepository.save(user);
        adminAuditLogService.log(
            "USER_PROFILE_UPDATE",
            "USER",
            updated.getId(),
            updated.getUsername(),
            "sensitive_fields_updated=true"
        );
        return toResponse(updated);
    }

    @Transactional
    public AdminUserResponse resetPassword(Long userId, ResetPasswordRequest request) {
        UserAccount user = findUser(userId);

        authSecurityService.validatePassword(request.password());

        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        UserAccount updated = userAccountRepository.save(user);
        adminAuditLogService.log(
            "USER_PASSWORD_RESET",
            "USER",
            updated.getId(),
            updated.getUsername(),
            "password_reset=true"
        );
        return toResponse(updated);
    }

    @Transactional
    public AdminUserResponse updateEnabled(Long userId, UpdateEnabledRequest request) {
        UserAccount user = findUser(userId);

        enforceAtLeastOneEnabledAdmin(user, user.getRoles(), request.enabled());

        user.setEnabled(request.enabled());
        if (!request.enabled()) {
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        }
        UserAccount updated = userAccountRepository.save(user);
        adminAuditLogService.log(
            "USER_ENABLED_UPDATE",
            "USER",
            updated.getId(),
            updated.getUsername(),
            "enabled=" + updated.isEnabled()
        );
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public Set<RoleName> allRoles() {
        return Set.of(RoleName.values());
    }

    private String normalizeUsername(String username) {
        if (username == null) {
            throw new ValidationException("Username is required");
        }

        String normalized = username.trim();
        if (normalized.isBlank()) {
            throw new ValidationException("Username is required");
        }
        return normalized;
    }

    private UserAccount findUser(Long userId) {
        return userAccountRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        if (roleNames == null || roleNames.isEmpty()) {
            throw new ValidationException("At least one role is required");
        }

        Set<Role> roles = roleRepository.findByNameIn(roleNames);
        if (roles.size() != roleNames.size()) {
            throw new ValidationException("One or more roles are invalid");
        }
        return roles;
    }

    private void enforceAtLeastOneEnabledAdmin(UserAccount targetUser, Set<Role> targetRoles, boolean targetEnabled) {
        boolean targetIsAdmin = targetRoles.stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (targetIsAdmin && targetEnabled) {
            return;
        }

        boolean currentlyAdmin = targetUser.getRoles().stream().anyMatch(role -> role.getName() == RoleName.ADMIN);
        if (!currentlyAdmin || !targetUser.isEnabled()) {
            return;
        }

        long enabledAdminCount = userAccountRepository.countDistinctByRoles_NameAndEnabledTrue(RoleName.ADMIN);
        if (enabledAdminCount <= 1) {
            throw new ValidationException("At least one enabled ADMIN user must remain");
        }
    }

    private AdminUserResponse toResponse(UserAccount user) {
        return new AdminUserResponse(
            user.getId(),
            user.getUsername(),
            user.isEnabled(),
            user.getLockedUntil(),
            sensitiveDataCryptoService.mask(sensitiveDataCryptoService.decrypt(user.getEmployeeIdentifierEncrypted())),
            sensitiveDataCryptoService.mask(sensitiveDataCryptoService.decrypt(user.getContactFieldEncrypted())),
            user.getRoles().stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toSet()),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }

    private String formatRoles(Set<Role> roles) {
        return roles.stream()
            .map(role -> role.getName().name())
            .sorted()
            .collect(Collectors.joining("|"));
    }
}
