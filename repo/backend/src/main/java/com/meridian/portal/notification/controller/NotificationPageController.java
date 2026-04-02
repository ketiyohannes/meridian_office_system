package com.meridian.portal.notification.controller;

import com.meridian.portal.notification.domain.NotificationTopic;
import java.util.Arrays;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class NotificationPageController {

    @GetMapping("/notifications")
    @PreAuthorize("hasAnyRole('REGULAR_USER','MERCHANDISER','OPS_MANAGER','ADMIN')")
    public String notifications(Authentication authentication, Model model) {
        List<String> topics = Arrays.stream(NotificationTopic.values()).map(Enum::name).toList();
        model.addAttribute("username", authentication.getName());
        model.addAttribute("topics", topics);
        return "notifications/index";
    }
}
