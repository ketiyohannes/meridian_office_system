CREATE TABLE recommendation_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    query_text VARCHAR(255) NULL,
    sku VARCHAR(64) NULL,
    category_name VARCHAR(100) NULL,
    region_code VARCHAR(40) NOT NULL DEFAULT 'GLOBAL',
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_reco_events_user_created (username, created_at),
    INDEX idx_reco_events_type_created (event_type, created_at),
    INDEX idx_reco_events_sku_created (sku, created_at),
    INDEX idx_reco_events_category_created (category_name, created_at)
);

CREATE TABLE recommendation_exposures (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    region_code VARCHAR(40) NOT NULL,
    surface VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_reco_exposure_user_created (username, created_at),
    INDEX idx_reco_exposure_region_sku_created (region_code, sku, created_at),
    INDEX idx_reco_exposure_sku_created (sku, created_at)
);

CREATE TABLE recommendation_daily_impressions (
    day_bucket DATE NOT NULL,
    region_code VARCHAR(40) NOT NULL,
    sku VARCHAR(64) NOT NULL,
    impressions INT NOT NULL DEFAULT 0,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (day_bucket, region_code, sku),
    INDEX idx_reco_daily_region_day (region_code, day_bucket)
);
