package com.meridian.portal.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        boolean canViewRecommendations = roles.contains("ROLE_ADMIN")
            || roles.contains("ROLE_MERCHANDISER")
            || roles.contains("ROLE_OPS_MANAGER");
        boolean canAccessPersonalWorkspace = roles.contains("ROLE_ADMIN")
            || roles.contains("ROLE_MERCHANDISER")
            || roles.contains("ROLE_OPS_MANAGER")
            || roles.contains("ROLE_REGULAR_USER");
        boolean hasUnifiedOperationsAccess = roles.contains("ROLE_ADMIN")
            || roles.contains("ROLE_OPS_MANAGER")
            || roles.contains("ROLE_ANALYST");
        model.addAttribute("username", authentication.getName());
        model.addAttribute("roles", roles);
        model.addAttribute("canViewRecommendations", canViewRecommendations);
        model.addAttribute("canAccessPersonalWorkspace", canAccessPersonalWorkspace);
        model.addAttribute("hasUnifiedOperationsAccess", hasUnifiedOperationsAccess);
        return "home";
    }

    @GetMapping("/admin")
    public String admin(Authentication authentication, Model model) {
        model.addAttribute("username", authentication.getName());
        return "admin";
    }
}
