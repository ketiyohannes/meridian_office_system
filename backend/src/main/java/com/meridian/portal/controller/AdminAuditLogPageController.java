package com.meridian.portal.controller;

import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.service.AdminAuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogPageController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogPageController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @GetMapping("/admin/audit-logs")
    public String auditLogs(
        @RequestParam(name = "actor", required = false) String actor,
        @RequestParam(name = "action", required = false) String action,
        @RequestParam(name = "targetUsername", required = false) String targetUsername,
        @RequestParam(name = "targetType", required = false) String targetType,
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        Model model
    ) {
        PagedResponse<?> logsPage;
        try {
            logsPage = adminAuditLogService.search(actor, action, targetUsername, targetType, from, to, page, size);
        } catch (ValidationException ex) {
            logsPage = adminAuditLogService.search(actor, action, targetUsername, targetType, null, null, 0, size);
            model.addAttribute("errorMessage", ex.getMessage());
        }

        model.addAttribute("logsPage", logsPage);
        model.addAttribute("logs", logsPage.content());
        model.addAttribute("actions", adminAuditLogService.actions());
        model.addAttribute("targetTypes", adminAuditLogService.targetTypes());
        model.addAttribute("actor", actor == null ? "" : actor);
        model.addAttribute("action", action == null ? "" : action);
        model.addAttribute("targetUsername", targetUsername == null ? "" : targetUsername);
        model.addAttribute("targetType", targetType == null ? "" : targetType);
        model.addAttribute("from", from == null ? "" : from);
        model.addAttribute("to", to == null ? "" : to);
        model.addAttribute("size", logsPage.size());
        model.addAttribute("currentPage", logsPage.page());
        model.addAttribute("totalPages", logsPage.totalPages());
        model.addAttribute("displayTotalPages", Math.max(logsPage.totalPages(), 1));
        return "admin/audit-logs";
    }
}
