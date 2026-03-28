package com.meridian.portal.notification.controller;

import com.meridian.portal.exception.ValidationException;
import com.meridian.portal.notification.domain.NotificationTopic;
import com.meridian.portal.notification.dto.EventDefinitionUpdateDto;
import com.meridian.portal.notification.dto.NotificationEventDefinitionResponse;
import com.meridian.portal.notification.dto.NotificationTemplateResponse;
import com.meridian.portal.notification.dto.TemplateUpdateDto;
import com.meridian.portal.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notifications")
@PreAuthorize("hasRole('ADMIN')")
public class AdminNotificationApiController {

    private final NotificationService notificationService;

    public AdminNotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping("/templates")
    public List<NotificationTemplateResponse> templates() {
        return notificationService.templates().stream()
            .map(t -> new NotificationTemplateResponse(
                t.getTopicCode().name(),
                t.getTitleTemplate(),
                t.getBodyTemplate(),
                t.isActive()
            ))
            .sorted(Comparator.comparing(NotificationTemplateResponse::topic))
            .toList();
    }

    @PutMapping("/templates/{topic}")
    public NotificationTemplateResponse updateTemplate(
        @PathVariable String topic,
        @Valid @RequestBody TemplateUpdateDto request
    ) {
        var updated = notificationService.updateTemplate(parseTopic(topic), request);
        return new NotificationTemplateResponse(
            updated.getTopicCode().name(),
            updated.getTitleTemplate(),
            updated.getBodyTemplate(),
            updated.isActive()
        );
    }

    @GetMapping("/definitions")
    public List<NotificationEventDefinitionResponse> definitions() {
        return notificationService.definitions().stream()
            .map(d -> new NotificationEventDefinitionResponse(
                d.getTopicCode().name(),
                d.getReminderIntervalMinutes(),
                d.getMaxReminders(),
                d.isActionable(),
                d.isActive()
            ))
            .sorted(Comparator.comparing(NotificationEventDefinitionResponse::topic))
            .toList();
    }

    @PutMapping("/definitions/{topic}")
    public NotificationEventDefinitionResponse updateDefinition(
        @PathVariable String topic,
        @Valid @RequestBody EventDefinitionUpdateDto request
    ) {
        var updated = notificationService.updateDefinition(parseTopic(topic), request);
        return new NotificationEventDefinitionResponse(
            updated.getTopicCode().name(),
            updated.getReminderIntervalMinutes(),
            updated.getMaxReminders(),
            updated.isActionable(),
            updated.isActive()
        );
    }

    private NotificationTopic parseTopic(String value) {
        try {
            return NotificationTopic.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            throw new ValidationException("Invalid topic: " + value);
        }
    }
}
