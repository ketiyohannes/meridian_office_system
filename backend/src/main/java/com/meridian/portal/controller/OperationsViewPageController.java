package com.meridian.portal.controller;

import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class OperationsViewPageController {

    @GetMapping("/operations")
    @PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER','ANALYST')")
    public String operationsView(Authentication auth, Model model) {
        Set<String> roles = auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .collect(Collectors.toSet());
        boolean canViewExceptions = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OPS_MANAGER");
        boolean canViewTaskWorkspace = roles.contains("ROLE_ADMIN") || roles.contains("ROLE_OPS_MANAGER");

        model.addAttribute("username", auth.getName());
        model.addAttribute("canViewExceptions", canViewExceptions);
        model.addAttribute("canViewTaskWorkspace", canViewTaskWorkspace);
        return "operations/index";
    }
}
