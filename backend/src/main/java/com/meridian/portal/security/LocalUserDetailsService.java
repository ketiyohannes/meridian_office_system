package com.meridian.portal.security;

import com.meridian.portal.domain.UserAccount;
import com.meridian.portal.repository.UserAccountRepository;
import com.meridian.portal.service.AuthSecurityService;
import java.util.stream.Collectors;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class LocalUserDetailsService implements UserDetailsService {

    private final UserAccountRepository userAccountRepository;
    private final AuthSecurityService authSecurityService;

    public LocalUserDetailsService(
        UserAccountRepository userAccountRepository,
        AuthSecurityService authSecurityService
    ) {
        this.userAccountRepository = userAccountRepository;
        this.authSecurityService = authSecurityService;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

        String[] authorities = user.getRoles().stream()
            .map(role -> role.getName().authority())
            .collect(Collectors.toSet())
            .toArray(String[]::new);

        return User.withUsername(user.getUsername())
            .password(user.getPasswordHash())
            .disabled(!user.isEnabled())
            .accountLocked(authSecurityService.isLocked(user))
            .authorities(authorities)
            .build();
    }
}
