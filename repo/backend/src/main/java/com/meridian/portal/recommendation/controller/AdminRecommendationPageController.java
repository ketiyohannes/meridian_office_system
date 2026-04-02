package com.meridian.portal.recommendation.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminRecommendationPageController {

    @GetMapping("/admin/recommendations")
    @PreAuthorize("hasRole('ADMIN')")
    public String recommendations() {
        return "admin/recommendations";
    }
}
