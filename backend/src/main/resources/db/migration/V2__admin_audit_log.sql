CREATE TABLE admin_audit_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    actor_username VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL,
    target_type VARCHAR(64) NOT NULL,
    target_id BIGINT NULL,
    target_username VARCHAR(64) NULL,
    details TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_admin_audit_logs_created_at (created_at),
    INDEX idx_admin_audit_logs_actor (actor_username),
    INDEX idx_admin_audit_logs_action (action)
);
