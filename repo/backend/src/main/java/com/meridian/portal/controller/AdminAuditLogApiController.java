package com.meridian.portal.controller;

import com.meridian.portal.dto.AdminAuditLogResponse;
import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.service.AdminAuditLogService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAuditLogApiController {

    private final AdminAuditLogService adminAuditLogService;

    public AdminAuditLogApiController(AdminAuditLogService adminAuditLogService) {
        this.adminAuditLogService = adminAuditLogService;
    }

    @GetMapping
    public PagedResponse<AdminAuditLogResponse> search(
        @RequestParam(name = "actor", required = false) String actor,
        @RequestParam(name = "action", required = false) String action,
        @RequestParam(name = "targetUsername", required = false) String targetUsername,
        @RequestParam(name = "targetType", required = false) String targetType,
        @RequestParam(name = "from", required = false) String from,
        @RequestParam(name = "to", required = false) String to,
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size
    ) {
        return adminAuditLogService.search(actor, action, targetUsername, targetType, from, to, page, size);
    }
}
