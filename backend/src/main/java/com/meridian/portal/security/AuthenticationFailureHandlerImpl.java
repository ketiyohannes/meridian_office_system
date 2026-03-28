package com.meridian.portal.security;

import com.meridian.portal.service.AuthSecurityService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class AuthenticationFailureHandlerImpl extends SimpleUrlAuthenticationFailureHandler {

    private final AuthSecurityService authSecurityService;

    public AuthenticationFailureHandlerImpl(AuthSecurityService authSecurityService) {
        this.authSecurityService = authSecurityService;
    }

    @Override
    public void onAuthenticationFailure(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException exception
    ) throws IOException, ServletException {
        String username = request.getParameter("username");
        authSecurityService.onAuthenticationFailure(username);

        if (isLockedException(exception)) {
            setDefaultFailureUrl("/login?locked");
        } else {
            setDefaultFailureUrl("/login?error");
        }

        super.onAuthenticationFailure(request, response, exception);
    }

    private boolean isLockedException(Throwable throwable) {
        Set<Throwable> visited = new HashSet<>();
        Throwable current = throwable;
        while (current != null && !visited.contains(current)) {
            if (current instanceof LockedException) {
                return true;
            }
            visited.add(current);
            current = current.getCause();
        }
        return false;
    }
}
