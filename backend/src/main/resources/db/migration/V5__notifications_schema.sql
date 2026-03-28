CREATE TABLE notification_templates (
    topic_code VARCHAR(64) NOT NULL PRIMARY KEY,
    title_template VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE notification_event_definitions (
    topic_code VARCHAR(64) NOT NULL PRIMARY KEY,
    reminder_interval_minutes INT NOT NULL DEFAULT 15,
    max_reminders INT NOT NULL DEFAULT 3,
    actionable TINYINT(1) NOT NULL DEFAULT 1,
    active TINYINT(1) NOT NULL DEFAULT 1,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE user_notification_preferences (
    username VARCHAR(64) NOT NULL PRIMARY KEY,
    dnd_start TIME NOT NULL DEFAULT '21:00:00',
    dnd_end TIME NOT NULL DEFAULT '07:00:00',
    max_reminders_per_event INT NOT NULL DEFAULT 3,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_notification_preferences_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

CREATE TABLE notification_subscriptions (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    topic_code VARCHAR(64) NOT NULL,
    subscribed TINYINT(1) NOT NULL DEFAULT 1,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_notification_subscriptions_user_topic UNIQUE (username, topic_code),
    CONSTRAINT fk_notification_subscriptions_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE
);

CREATE TABLE notification_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_key VARCHAR(100) NOT NULL,
    username VARCHAR(64) NOT NULL,
    topic_code VARCHAR(64) NOT NULL,
    event_time DATETIME(6) NOT NULL,
    payload_json TEXT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reminder_count INT NOT NULL DEFAULT 0,
    max_reminders INT NOT NULL DEFAULT 3,
    next_attempt_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_notification_events UNIQUE (event_key, username, topic_code),
    CONSTRAINT fk_notification_events_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,
    INDEX idx_notification_events_status_next_attempt (status, next_attempt_at),
    INDEX idx_notification_events_user (username)
);

CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    topic_code VARCHAR(64) NOT NULL,
    event_key VARCHAR(100) NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    sent_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    read_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'SENT',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_notifications_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,
    INDEX idx_notifications_user_sent_at (username, sent_at),
    INDEX idx_notifications_user_status (username, status)
);

INSERT INTO notification_templates (topic_code, title_template, body_template, active) VALUES
('CHECKIN_WINDOW_OPEN', 'Check-in window open', 'Check-in opens at {{time}}.', 1),
('CHECKIN_CUTOFF_WARNING', 'Check-in cutoff soon', 'Check-in cutoff is in 15 minutes at {{time}}.', 1),
('CHECKIN_MISSED', 'Check-in missed', 'You missed your check-in window for {{time}}.', 1),
('EXCEPTION_APPROVAL_OUTCOME', 'Exception request update', 'Your exception request status: {{outcome}}.', 1);

INSERT INTO notification_event_definitions (topic_code, reminder_interval_minutes, max_reminders, actionable, active) VALUES
('CHECKIN_WINDOW_OPEN', 15, 3, 0, 1),
('CHECKIN_CUTOFF_WARNING', 5, 3, 0, 1),
('CHECKIN_MISSED', 30, 1, 0, 1),
('EXCEPTION_APPROVAL_OUTCOME', 30, 1, 1, 1);
