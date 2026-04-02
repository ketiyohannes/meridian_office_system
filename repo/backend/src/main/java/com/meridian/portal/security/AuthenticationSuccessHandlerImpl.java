package com.meridian.portal.security;

import com.meridian.portal.service.AuthSecurityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationSuccessHandlerImpl extends SavedRequestAwareAuthenticationSuccessHandler {

    private final AuthSecurityService authSecurityService;

    public AuthenticationSuccessHandlerImpl(AuthSecurityService authSecurityService) {
        this.authSecurityService = authSecurityService;
        setDefaultTargetUrl("/");
    }

    @Override
    public void onAuthenticationSuccess(
        HttpServletRequest request,
        HttpServletResponse response,
        Authentication authentication
    ) throws ServletException, IOException {
        authSecurityService.onAuthenticationSuccess(authentication.getName());
        super.onAuthenticationSuccess(request, response, authentication);
    }
}
