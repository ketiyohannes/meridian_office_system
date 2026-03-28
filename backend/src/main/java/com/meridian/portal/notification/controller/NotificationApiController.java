package com.meridian.portal.notification.controller;

import com.meridian.portal.dto.PagedResponse;
import com.meridian.portal.notification.dto.NotificationResponse;
import com.meridian.portal.notification.dto.SubscriptionDto;
import com.meridian.portal.notification.dto.UserNotificationPreferenceDto;
import com.meridian.portal.notification.service.NotificationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notifications")
@PreAuthorize("hasAnyRole('REGULAR_USER','MERCHANDISER','OPS_MANAGER','ADMIN')")
public class NotificationApiController {

    private final NotificationService notificationService;

    public NotificationApiController(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public PagedResponse<NotificationResponse> myNotifications(
        @RequestParam(name = "page", defaultValue = "0") int page,
        @RequestParam(name = "size", defaultValue = "20") int size,
        Authentication auth
    ) {
        return notificationService.myNotifications(auth, page, size);
    }

    @GetMapping("/unread-count")
    public long unreadCount(Authentication auth) {
        return notificationService.unreadCount(auth);
    }

    @PutMapping("/{notificationId}/read")
    public void markRead(@PathVariable Long notificationId, Authentication auth) {
        notificationService.markRead(auth, notificationId);
    }

    @PutMapping("/read-all")
    public int markAllRead(Authentication auth) {
        return notificationService.markAllRead(auth);
    }

    @GetMapping("/preferences")
    public UserNotificationPreferenceDto myPreferences(Authentication auth) {
        return notificationService.myPreferences(auth);
    }

    @PutMapping("/preferences")
    public UserNotificationPreferenceDto updatePreferences(
        @Valid @RequestBody UserNotificationPreferenceDto input,
        Authentication auth
    ) {
        return notificationService.updatePreferences(auth, input);
    }

    @GetMapping("/subscriptions")
    public List<SubscriptionDto> mySubscriptions(Authentication auth) {
        return notificationService.mySubscriptions(auth);
    }

    @PutMapping("/subscriptions")
    public List<SubscriptionDto> updateSubscriptions(
        @Valid @RequestBody List<@Valid SubscriptionDto> updates,
        Authentication auth
    ) {
        return notificationService.updateSubscriptions(auth, updates);
    }
}
