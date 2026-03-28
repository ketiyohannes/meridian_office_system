package com.meridian.portal.controller;

import com.meridian.portal.domain.RoleName;
import com.meridian.portal.dto.CreateUserForm;
import com.meridian.portal.dto.CreateUserRequest;
import com.meridian.portal.dto.ResetPasswordForm;
import com.meridian.portal.dto.ResetPasswordRequest;
import com.meridian.portal.dto.UpdateEnabledForm;
import com.meridian.portal.dto.UpdateEnabledRequest;
import com.meridian.portal.dto.UpdateUserProfileForm;
import com.meridian.portal.dto.UpdateUserProfileRequest;
import com.meridian.portal.dto.UpdateRolesForm;
import com.meridian.portal.dto.UpdateRolesRequest;
import com.meridian.portal.exception.ConflictException;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.service.AdminUserManagementService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserPageController {

    private final AdminUserManagementService adminUserManagementService;

    public AdminUserPageController(AdminUserManagementService adminUserManagementService) {
        this.adminUserManagementService = adminUserManagementService;
    }

    @GetMapping("/admin/users")
    public String usersPage(Model model) {
        if (!model.containsAttribute("createUserForm")) {
            model.addAttribute("createUserForm", new CreateUserForm());
        }

        model.addAttribute("users", adminUserManagementService.listUsers());
        model.addAttribute("allRoles", adminUserManagementService.allRoles());
        return "admin/users";
    }

    @PostMapping("/admin/users")
    public String createUser(
        @Valid @ModelAttribute("createUserForm") CreateUserForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        validatePasswordConfirmation(form.getPassword(), form.getConfirmPassword(), "confirmPassword", bindingResult);

        if (bindingResult.hasErrors()) {
            model.addAttribute("users", adminUserManagementService.listUsers());
            model.addAttribute("allRoles", adminUserManagementService.allRoles());
            return "admin/users";
        }

        try {
            adminUserManagementService.createUser(new CreateUserRequest(
                form.getUsername(),
                form.getPassword(),
                form.getRoles(),
                form.isEnabled(),
                form.getEmployeeIdentifier(),
                form.getContactField()
            ));
            redirectAttributes.addFlashAttribute("successMessage", "User created successfully.");
            return "redirect:/admin/users";
        } catch (ConflictException | ValidationException ex) {
            bindingResult.reject("createUserError", ex.getMessage());
            model.addAttribute("users", adminUserManagementService.listUsers());
            model.addAttribute("allRoles", adminUserManagementService.allRoles());
            return "admin/users";
        }
    }

    @GetMapping("/admin/users/{userId}")
    public String userDetailPage(@PathVariable Long userId, Model model) {
        if (!model.containsAttribute("rolesForm")) {
            UpdateRolesForm rolesForm = new UpdateRolesForm();
            rolesForm.setRoles(asRoleNames(adminUserManagementService.getUser(userId).roles()));
            model.addAttribute("rolesForm", rolesForm);
        }

        if (!model.containsAttribute("passwordForm")) {
            model.addAttribute("passwordForm", new ResetPasswordForm());
        }

        if (!model.containsAttribute("enabledForm")) {
            UpdateEnabledForm enabledForm = new UpdateEnabledForm();
            enabledForm.setEnabled(adminUserManagementService.getUser(userId).enabled());
            model.addAttribute("enabledForm", enabledForm);
        }
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", new UpdateUserProfileForm());
        }

        model.addAttribute("allRoles", adminUserManagementService.allRoles());
        model.addAttribute("managedUser", adminUserManagementService.getUser(userId));
        return "admin/user-detail";
    }

    @PostMapping("/admin/users/{userId}/roles")
    public String updateRoles(
        @PathVariable Long userId,
        @Valid @ModelAttribute("rolesForm") UpdateRolesForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            loadDetailModel(model, userId);
            model.addAttribute("passwordForm", new ResetPasswordForm());
            UpdateEnabledForm enabledForm = new UpdateEnabledForm();
            enabledForm.setEnabled(adminUserManagementService.getUser(userId).enabled());
            model.addAttribute("enabledForm", enabledForm);
            return "admin/user-detail";
        }

        try {
            adminUserManagementService.updateRoles(userId, new UpdateRolesRequest(form.getRoles()));
            redirectAttributes.addFlashAttribute("successMessage", "Roles updated.");
            return "redirect:/admin/users/" + userId;
        } catch (ValidationException ex) {
            bindingResult.reject("rolesError", ex.getMessage());
            loadDetailModel(model, userId);
            model.addAttribute("passwordForm", new ResetPasswordForm());
            UpdateEnabledForm enabledForm = new UpdateEnabledForm();
            enabledForm.setEnabled(adminUserManagementService.getUser(userId).enabled());
            model.addAttribute("enabledForm", enabledForm);
            return "admin/user-detail";
        }
    }

    @PostMapping("/admin/users/{userId}/password")
    public String resetPassword(
        @PathVariable Long userId,
        @Valid @ModelAttribute("passwordForm") ResetPasswordForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        validatePasswordConfirmation(form.getPassword(), form.getConfirmPassword(), "confirmPassword", bindingResult);

        if (bindingResult.hasErrors()) {
            loadDetailModel(model, userId);
            UpdateRolesForm rolesForm = new UpdateRolesForm();
            rolesForm.setRoles(asRoleNames(adminUserManagementService.getUser(userId).roles()));
            model.addAttribute("rolesForm", rolesForm);
            UpdateEnabledForm enabledForm = new UpdateEnabledForm();
            enabledForm.setEnabled(adminUserManagementService.getUser(userId).enabled());
            model.addAttribute("enabledForm", enabledForm);
            return "admin/user-detail";
        }

        try {
            adminUserManagementService.resetPassword(userId, new ResetPasswordRequest(form.getPassword()));
            redirectAttributes.addFlashAttribute("successMessage", "Password reset successfully.");
            return "redirect:/admin/users/" + userId;
        } catch (ValidationException ex) {
            bindingResult.reject("passwordError", ex.getMessage());
            loadDetailModel(model, userId);
            UpdateRolesForm rolesForm = new UpdateRolesForm();
            rolesForm.setRoles(asRoleNames(adminUserManagementService.getUser(userId).roles()));
            model.addAttribute("rolesForm", rolesForm);
            UpdateEnabledForm enabledForm = new UpdateEnabledForm();
            enabledForm.setEnabled(adminUserManagementService.getUser(userId).enabled());
            model.addAttribute("enabledForm", enabledForm);
            return "admin/user-detail";
        }
    }

    @PostMapping("/admin/users/{userId}/enabled")
    public String updateEnabled(
        @PathVariable Long userId,
        @ModelAttribute("enabledForm") UpdateEnabledForm form,
        RedirectAttributes redirectAttributes
    ) {
        try {
            adminUserManagementService.updateEnabled(userId, new UpdateEnabledRequest(form.isEnabled()));
            redirectAttributes.addFlashAttribute("successMessage", "User status updated.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users/" + userId;
    }

    @PostMapping("/admin/users/{userId}/profile")
    public String updateProfile(
        @PathVariable Long userId,
        @Valid @ModelAttribute("profileForm") UpdateUserProfileForm form,
        BindingResult bindingResult,
        Model model,
        RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            loadDetailModel(model, userId);
            return "admin/user-detail";
        }
        try {
            adminUserManagementService.updateProfile(
                userId,
                new UpdateUserProfileRequest(form.getEmployeeIdentifier(), form.getContactField())
            );
            redirectAttributes.addFlashAttribute("successMessage", "Sensitive profile fields updated.");
        } catch (ValidationException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/admin/users/" + userId;
    }

    private void validatePasswordConfirmation(
        String password,
        String confirmPassword,
        String field,
        BindingResult bindingResult
    ) {
        if (password != null && !password.equals(confirmPassword)) {
            bindingResult.rejectValue(field, "passwordMismatch", "Passwords do not match");
        }
    }

    private void loadDetailModel(Model model, Long userId) {
        model.addAttribute("managedUser", adminUserManagementService.getUser(userId));
        model.addAttribute("allRoles", adminUserManagementService.allRoles());
        if (!model.containsAttribute("profileForm")) {
            model.addAttribute("profileForm", new UpdateUserProfileForm());
        }
    }

    private Set<RoleName> asRoleNames(Set<String> roles) {
        return roles.stream().map(RoleName::valueOf).collect(java.util.stream.Collectors.toSet());
    }
}
