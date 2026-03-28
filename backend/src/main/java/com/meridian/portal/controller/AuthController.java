package com.meridian.portal.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    @GetMapping("/login")
    public String login(
        @RequestParam(name = "error", required = false) String error,
        @RequestParam(name = "locked", required = false) String locked,
        @RequestParam(name = "logout", required = false) String logout,
        Model model
    ) {
        model.addAttribute("error", error != null);
        model.addAttribute("locked", locked != null);
        model.addAttribute("logout", logout != null);
        return "login";
    }
}
