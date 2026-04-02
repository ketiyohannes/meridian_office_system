package com.meridian.portal.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ExceptionOpsPageController {

    @GetMapping("/ops/exceptions")
    @PreAuthorize("hasAnyRole('OPS_MANAGER','ADMIN')")
    public String exceptionOpsPage(Authentication auth, Model model) {
        boolean canDecide = auth.getAuthorities().stream()
            .map(a -> a.getAuthority())
            .anyMatch(role -> "ROLE_ADMIN".equals(role) || "ROLE_OPS_MANAGER".equals(role));
        model.addAttribute("canDecide", canDecide);
        return "ops/exceptions";
    }
}
