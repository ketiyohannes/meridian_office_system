package com.meridian.portal.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "notification_events")
public class NotificationEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 100)
    private String eventKey;

    @Column(name = "username", nullable = false, length = 64)
    private String username;

    @Enumerated(EnumType.STRING)
    @Column(name = "topic_code", nullable = false, length = 64)
    private NotificationTopic topicCode;

    @Column(name = "event_time", nullable = false)
    private Instant eventTime;

    @Column(name = "payload_json")
    private String payloadJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private NotificationEventStatus status;

    @Column(name = "reminder_count", nullable = false)
    private int reminderCount;

    @Column(name = "max_reminders", nullable = false)
    private int maxReminders;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    public void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) {
            status = NotificationEventStatus.PENDING;
        }
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getEventKey() {
        return eventKey;
    }

    public void setEventKey(String eventKey) {
        this.eventKey = eventKey;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public NotificationTopic getTopicCode() {
        return topicCode;
    }

    public void setTopicCode(NotificationTopic topicCode) {
        this.topicCode = topicCode;
    }

    public Instant getEventTime() {
        return eventTime;
    }

    public void setEventTime(Instant eventTime) {
        this.eventTime = eventTime;
    }

    public String getPayloadJson() {
        return payloadJson;
    }

    public void setPayloadJson(String payloadJson) {
        this.payloadJson = payloadJson;
    }

    public NotificationEventStatus getStatus() {
        return status;
    }

    public void setStatus(NotificationEventStatus status) {
        this.status = status;
    }

    public int getReminderCount() {
        return reminderCount;
    }

    public void setReminderCount(int reminderCount) {
        this.reminderCount = reminderCount;
    }

    public int getMaxReminders() {
        return maxReminders;
    }

    public void setMaxReminders(int maxReminders) {
        this.maxReminders = maxReminders;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public void setNextAttemptAt(Instant nextAttemptAt) {
        this.nextAttemptAt = nextAttemptAt;
    }
}
