package com.meridian.portal.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.meridian.portal.security.AuthenticationFailureHandlerImpl;
import com.meridian.portal.config.AuthProperties;
import com.meridian.portal.service.AuthSecurityService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AuthenticationFailureHandlerImplTest {

    @Test
    void redirectsToLockedWhenLockedExceptionExistsInCauseChain() throws Exception {
        AuthSecurityService authSecurityService = new AuthSecurityService(
            null,
            null,
            null,
            new AuthProperties()
        );
        AuthenticationFailureHandlerImpl handler = new AuthenticationFailureHandlerImpl(authSecurityService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(
            request,
            response,
            new InternalAuthenticationServiceException("wrapped", new LockedException("locked"))
        );

        assertEquals("/login?locked", response.getRedirectedUrl());
    }

    @Test
    void redirectsToErrorForGenericAuthenticationFailure() throws Exception {
        AuthSecurityService authSecurityService = new AuthSecurityService(
            null,
            null,
            null,
            new AuthProperties()
        );
        AuthenticationFailureHandlerImpl handler = new AuthenticationFailureHandlerImpl(authSecurityService);
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationFailure(request, response, new BadCredentialsException("bad creds"));

        assertEquals("/login?error", response.getRedirectedUrl());
    }
}
