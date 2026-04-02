CREATE TABLE health_thresholds (
    metric_code VARCHAR(64) NOT NULL PRIMARY KEY,
    window_minutes INT NOT NULL,
    threshold_percent DECIMAL(6,3) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE health_minute_metrics (
    bucket_start DATETIME(0) NOT NULL PRIMARY KEY,
    total_requests INT NOT NULL DEFAULT 0,
    error_5xx_requests INT NOT NULL DEFAULT 0,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE health_alerts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    metric_code VARCHAR(64) NOT NULL,
    window_minutes INT NOT NULL,
    threshold_percent DECIMAL(6,3) NOT NULL,
    actual_percent DECIMAL(6,3) NOT NULL,
    bucket_start DATETIME(0) NOT NULL,
    resolved TINYINT(1) NOT NULL DEFAULT 0,
    resolved_at DATETIME(6) NULL,
    details_json TEXT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT uq_health_alert_metric_bucket UNIQUE (metric_code, bucket_start),
    INDEX idx_health_alerts_metric_created (metric_code, created_at),
    INDEX idx_health_alerts_resolved_created (resolved, created_at)
);

CREATE TABLE health_error_logs (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    status_code INT NOT NULL,
    request_path VARCHAR(255) NOT NULL,
    message VARCHAR(500) NOT NULL,
    details_json TEXT NULL,
    occurred_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_health_error_logs_occurred (occurred_at),
    INDEX idx_health_error_logs_status (status_code)
);

INSERT INTO health_thresholds (metric_code, window_minutes, threshold_percent, enabled) VALUES
('HTTP_5XX_RATE', 15, 1.000, 1);

CREATE TABLE order_facts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    order_number VARCHAR(64) NOT NULL UNIQUE,
    user_username VARCHAR(64) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    product_sku VARCHAR(64) NOT NULL,
    category_name VARCHAR(100) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    region_code VARCHAR(40) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    quantity INT NOT NULL,
    order_status VARCHAR(30) NOT NULL,
    cancellation_reason VARCHAR(120) NULL,
    ordered_at DATETIME(6) NOT NULL,
    fulfilled_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_order_facts_ordered_at (ordered_at),
    INDEX idx_order_facts_category (category_name),
    INDEX idx_order_facts_channel (channel),
    INDEX idx_order_facts_role (user_role),
    INDEX idx_order_facts_region (region_code),
    INDEX idx_order_facts_status (order_status)
);

CREATE TABLE analytics_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    channel VARCHAR(40) NOT NULL,
    region_code VARCHAR(40) NOT NULL,
    user_role VARCHAR(50) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_analytics_events_created (created_at),
    INDEX idx_analytics_events_type (event_type),
    INDEX idx_analytics_events_region (region_code)
);

INSERT INTO order_facts (
    order_number, user_username, user_role, product_sku, category_name, channel, region_code, unit_price, quantity,
    order_status, cancellation_reason, ordered_at, fulfilled_at
) VALUES
('ORD-1001', 'admin', 'ADMIN', 'SKU-1001', 'Electronics', 'STORE', 'NE-1', 120.00, 1, 'FULFILLED', NULL, DATE_SUB(NOW(), INTERVAL 2 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY)),
('ORD-1002', 'admin', 'ADMIN', 'SKU-1002', 'Home', 'STORE', 'NE-1', 80.00, 2, 'FULFILLED', NULL, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_SUB(NOW(), INTERVAL 2 DAY)),
('ORD-1003', 'admin', 'ADMIN', 'SKU-1003', 'Electronics', 'ONLINE', 'SW-2', 250.00, 1, 'CANCELLED', 'OUT_OF_STOCK', DATE_SUB(NOW(), INTERVAL 1 DAY), NULL),
('ORD-1004', 'admin', 'ADMIN', 'SKU-1004', 'Sports', 'STORE', 'NE-1', 45.00, 3, 'FULFILLED', NULL, DATE_SUB(NOW(), INTERVAL 4 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('ORD-1005', 'admin', 'ADMIN', 'SKU-1005', 'Electronics', 'ONLINE', 'SW-2', 99.00, 1, 'FULFILLED', NULL, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_SUB(NOW(), INTERVAL 3 DAY)),
('ORD-1006', 'admin', 'ADMIN', 'SKU-1006', 'Home', 'STORE', 'NE-1', 55.00, 1, 'CANCELLED', 'PAYMENT_FAILURE', DATE_SUB(NOW(), INTERVAL 2 DAY), NULL);

INSERT INTO analytics_events (event_type, channel, region_code, user_role, created_at) VALUES
('STORE_VISIT', 'STORE', 'NE-1', 'REGULAR_USER', DATE_SUB(NOW(), INTERVAL 2 DAY)),
('STORE_VISIT', 'STORE', 'NE-1', 'REGULAR_USER', DATE_SUB(NOW(), INTERVAL 2 DAY)),
('STORE_VISIT', 'STORE', 'NE-1', 'REGULAR_USER', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('STORE_VISIT', 'ONLINE', 'SW-2', 'REGULAR_USER', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('STORE_VISIT', 'ONLINE', 'SW-2', 'REGULAR_USER', DATE_SUB(NOW(), INTERVAL 1 DAY)),
('STORE_VISIT', 'ONLINE', 'SW-2', 'REGULAR_USER', DATE_SUB(NOW(), INTERVAL 1 DAY));
