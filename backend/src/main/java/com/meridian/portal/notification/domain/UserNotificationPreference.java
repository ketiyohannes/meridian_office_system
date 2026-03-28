package com.meridian.portal.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalTime;

@Entity
@Table(name = "user_notification_preferences")
public class UserNotificationPreference {

    @Id
    @Column(name = "username", length = 64)
    private String username;

    @Column(name = "dnd_start", nullable = false)
    private LocalTime dndStart;

    @Column(name = "dnd_end", nullable = false)
    private LocalTime dndEnd;

    @Column(name = "max_reminders_per_event", nullable = false)
    private int maxRemindersPerEvent;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        updatedAt = Instant.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public LocalTime getDndStart() {
        return dndStart;
    }

    public void setDndStart(LocalTime dndStart) {
        this.dndStart = dndStart;
    }

    public LocalTime getDndEnd() {
        return dndEnd;
    }

    public void setDndEnd(LocalTime dndEnd) {
        this.dndEnd = dndEnd;
    }

    public int getMaxRemindersPerEvent() {
        return maxRemindersPerEvent;
    }

    public void setMaxRemindersPerEvent(int maxRemindersPerEvent) {
        this.maxRemindersPerEvent = maxRemindersPerEvent;
    }
}
