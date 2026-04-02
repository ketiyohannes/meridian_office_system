package com.meridian.portal.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class TaskPageController {

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('REGULAR_USER','MERCHANDISER','OPS_MANAGER','ADMIN')")
    public String tasks(Authentication auth, Model model) {
        model.addAttribute("username", auth.getName());
        return "tasks/index";
    }
}
