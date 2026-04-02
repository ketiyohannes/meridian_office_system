package com.meridian.portal.discovery.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DiscoveryRulePageController {

    @GetMapping("/discovery/rules")
    @PreAuthorize("hasAnyRole('ADMIN','MERCHANDISER')")
    public String rulesPage() {
        return "discovery/rules";
    }
}
