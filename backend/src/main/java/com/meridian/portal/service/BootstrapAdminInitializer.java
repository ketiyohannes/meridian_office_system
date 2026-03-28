package com.meridian.portal.service;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class BootstrapAdminInitializer implements ApplicationRunner {

    private final AuthSecurityService authSecurityService;

    public BootstrapAdminInitializer(AuthSecurityService authSecurityService) {
        this.authSecurityService = authSecurityService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authSecurityService.bootstrapSecurityData();
    }
}
