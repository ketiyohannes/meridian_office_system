CREATE TABLE recommendation_config (
    config_key VARCHAR(64) NOT NULL PRIMARY KEY,
    config_value VARCHAR(64) NOT NULL,
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

INSERT INTO recommendation_config (config_key, config_value) VALUES
('DEDUPE_HOURS', '24'),
('LONG_TAIL_PERCENT', '20'),
('MAX_DAILY_IMPRESSIONS_PER_REGION', '200'),
('MAX_LIMIT', '30'),
('SCORING_WINDOW_DAYS', '30'),
('LONG_TAIL_WINDOW_DAYS', '7'),
('COLD_START_CATEGORY_LIMIT', '8'),
('MIN_CATEGORY_DIVERSITY', '3');

DROP INDEX idx_products_condition_status ON products;
DROP INDEX idx_products_zip_code ON products;
DROP INDEX idx_search_query_events_user_created ON search_query_events;
