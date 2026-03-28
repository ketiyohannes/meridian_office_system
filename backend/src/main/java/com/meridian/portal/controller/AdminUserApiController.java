package com.meridian.portal.controller;

import com.meridian.portal.dto.AdminUserResponse;
import com.meridian.portal.dto.CreateUserRequest;
import com.meridian.portal.dto.ResetPasswordRequest;
import com.meridian.portal.dto.UpdateEnabledRequest;
import com.meridian.portal.dto.UpdateUserProfileRequest;
import com.meridian.portal.dto.UpdateRolesRequest;
import com.meridian.portal.service.AdminUserManagementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserApiController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserApiController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping
    public List<AdminUserResponse> listUsers() {
        return adminUserManagementService.listUsers();
    }

    @GetMapping("/{userId}")
    public AdminUserResponse getUser(@PathVariable Long userId) {
        return adminUserManagementService.getUser(userId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminUserResponse createUser(@Valid @RequestBody CreateUserRequest request) {
        return adminUserManagementService.createUser(request);
    }

    @PutMapping("/{userId}/roles")
    public AdminUserResponse updateRoles(@PathVariable Long userId, @Valid @RequestBody UpdateRolesRequest request) {
        return adminUserManagementService.updateRoles(userId, request);
    }

    @PutMapping("/{userId}/password")
    public AdminUserResponse resetPassword(@PathVariable Long userId, @Valid @RequestBody ResetPasswordRequest request) {
        return adminUserManagementService.resetPassword(userId, request);
    }

    @PutMapping("/{userId}/enabled")
    public AdminUserResponse updateEnabled(@PathVariable Long userId, @Valid @RequestBody UpdateEnabledRequest request) {
        return adminUserManagementService.updateEnabled(userId, request);
    }

    @PutMapping("/{userId}/profile")
    public AdminUserResponse updateProfile(@PathVariable Long userId, @Valid @RequestBody UpdateUserProfileRequest request) {
        return adminUserManagementService.updateProfile(userId, request);
    }
}
