package com.meridian.portal.notification.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminNotificationPageController {

    @GetMapping("/admin/notifications")
    @PreAuthorize("hasRole('ADMIN')")
    public String notificationsAdmin() {
        return "notifications/admin";
    }
}
