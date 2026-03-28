package com.meridian.portal.health.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminHealthPageController {

    @GetMapping("/admin/health")
    @PreAuthorize("hasRole('ADMIN')")
    public String health() {
        return "admin/health";
    }
}
