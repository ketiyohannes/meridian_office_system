CREATE INDEX idx_products_enabled_posted_at ON products(enabled, posted_at);
CREATE INDEX idx_products_category_price ON products(category_id, price);
CREATE INDEX idx_products_condition_status ON products(condition_status);
CREATE INDEX idx_products_zip_code ON products(zip_code);
CREATE INDEX idx_search_query_events_user_created ON search_query_events(username, created_at);
CREATE INDEX idx_search_query_events_query_created ON search_query_events(query_text, created_at);
