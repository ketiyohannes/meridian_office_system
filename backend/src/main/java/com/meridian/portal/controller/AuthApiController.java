package com.meridian.portal.controller;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthApiController {

    @GetMapping("/me")
    public CurrentUserResponse me(Authentication authentication) {
        List<String> roles = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .toList();
        return new CurrentUserResponse(authentication.getName(), roles);
    }

    public record CurrentUserResponse(String username, List<String> roles) {}
}
