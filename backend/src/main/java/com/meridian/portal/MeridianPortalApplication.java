package com.meridian.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class MeridianPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeridianPortalApplication.class, args);
    }
}
