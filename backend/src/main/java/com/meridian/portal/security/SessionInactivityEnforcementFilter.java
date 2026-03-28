package com.meridian.portal.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionInactivityEnforcementFilter extends OncePerRequestFilter {

    public static final String LAST_ACTIVITY_ATTRIBUTE = "MERIDIAN_LAST_ACTIVITY_MS";

    private final long inactivityTimeoutMillis;

    public SessionInactivityEnforcementFilter(@Value("${server.servlet.session.timeout:30m}") Duration inactivityTimeout) {
        this.inactivityTimeoutMillis = inactivityTimeout.toMillis();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String uri = request.getRequestURI();
        return uri.equals("/login")
            || uri.equals("/logout")
            || uri.startsWith("/css/")
            || uri.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null && inactivityTimeoutMillis > 0) {
            Object lastActivityValue = session.getAttribute(LAST_ACTIVITY_ATTRIBUTE);
            long now = System.currentTimeMillis();
            if (lastActivityValue instanceof Long lastActivity && (now - lastActivity) > inactivityTimeoutMillis) {
                session.invalidate();
                SecurityContextHolder.clearContext();
                response.sendRedirect(request.getContextPath() + "/login?expired");
                return;
            }
            session.setAttribute(LAST_ACTIVITY_ATTRIBUTE, now);
        }
        filterChain.doFilter(request, response);
    }
}
