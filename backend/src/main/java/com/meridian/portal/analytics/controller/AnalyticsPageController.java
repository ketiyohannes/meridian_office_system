package com.meridian.portal.analytics.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AnalyticsPageController {

    @GetMapping("/analytics")
    @PreAuthorize("hasAnyRole('ADMIN','ANALYST','OPS_MANAGER')")
    public String analytics() {
        return "analytics/index";
    }
}
