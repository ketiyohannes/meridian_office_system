ALTER TABLE admin_audit_logs
    ADD INDEX idx_admin_audit_logs_target_username (target_username),
    ADD INDEX idx_admin_audit_logs_target_type (target_type),
    ADD INDEX idx_admin_audit_logs_target_type_username (target_type, target_username);
