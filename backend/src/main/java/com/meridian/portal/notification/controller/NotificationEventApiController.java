package com.meridian.portal.notification.controller;

import com.meridian.portal.notification.dto.ApprovalOutcomeRequest;
import com.meridian.portal.notification.dto.CheckinEventRequest;
import com.meridian.portal.notification.service.NotificationService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications/events")
@PreAuthorize("hasAnyRole('ADMIN','OPS_MANAGER')")
public class NotificationEventApiController {

    private final NotificationService notificationService;

    public NotificationEventApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @PostMapping("/checkin")
    public void createCheckinEvents(@Valid @RequestBody CheckinEventRequest request) {
        notificationService.createCheckinEvents(request);
    }

    @PostMapping("/approval-outcome")
    public void createApprovalOutcome(@Valid @RequestBody ApprovalOutcomeRequest request) {
        notificationService.createApprovalOutcomeEvent(request);
    }
}
