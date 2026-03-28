CREATE TABLE regions (
    region_code VARCHAR(40) NOT NULL PRIMARY KEY,
    region_name VARCHAR(100) NOT NULL,
    city_name VARCHAR(100) NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

INSERT INTO regions (region_code, region_name, city_name, active) VALUES
('NE-1', 'North East Region 1', 'New York', 1),
('SW-2', 'South West Region 2', 'Los Angeles', 1),
('GLOBAL', 'Global', NULL, 1);

CREATE TABLE discovery_rules (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    rule_type VARCHAR(32) NOT NULL,
    match_value VARCHAR(120) NULL,
    target_value VARCHAR(120) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_discovery_rules_type_active_priority (rule_type, active, priority),
    INDEX idx_discovery_rules_target (target_value)
);

CREATE TABLE user_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT NULL,
    due_at DATETIME(6) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by VARCHAR(64) NOT NULL,
    completed_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_user_tasks_user FOREIGN KEY (username) REFERENCES users(username) ON DELETE CASCADE,
    INDEX idx_user_tasks_username_status_due (username, status, due_at),
    INDEX idx_user_tasks_created_at (created_at)
);

CREATE TABLE exception_requests (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    request_key VARCHAR(64) NOT NULL UNIQUE,
    requester_username VARCHAR(64) NOT NULL,
    request_type VARCHAR(64) NOT NULL,
    details TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    decided_by VARCHAR(64) NULL,
    decision_comment VARCHAR(500) NULL,
    decided_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_exception_requests_user FOREIGN KEY (requester_username) REFERENCES users(username) ON DELETE CASCADE,
    INDEX idx_exception_requests_status_created (status, created_at),
    INDEX idx_exception_requests_requester_created (requester_username, created_at)
);
