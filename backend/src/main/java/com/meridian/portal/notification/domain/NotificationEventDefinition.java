package com.meridian.portal.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_event_definitions")
public class NotificationEventDefinition {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(name = "topic_code", length = 64)
    private NotificationTopic topicCode;

    @Column(name = "reminder_interval_minutes", nullable = false)
    private int reminderIntervalMinutes;

    @Column(name = "max_reminders", nullable = false)
    private int maxReminders;

    @Column(name = "actionable", nullable = false)
    private boolean actionable;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public NotificationTopic getTopicCode() {
        return topicCode;
    }

    public int getReminderIntervalMinutes() {
        return reminderIntervalMinutes;
    }

    public void setReminderIntervalMinutes(int reminderIntervalMinutes) {
        this.reminderIntervalMinutes = reminderIntervalMinutes;
    }

    public int getMaxReminders() {
        return maxReminders;
    }

    public void setMaxReminders(int maxReminders) {
        this.maxReminders = maxReminders;
    }

    public boolean isActionable() {
        return actionable;
    }

    public void setActionable(boolean actionable) {
        this.actionable = actionable;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
