CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

CREATE TABLE zip_reference (
    zip_code VARCHAR(10) NOT NULL PRIMARY KEY,
    latitude DECIMAL(10,6) NOT NULL,
    longitude DECIMAL(10,6) NOT NULL,
    city VARCHAR(100) NULL,
    state_code VARCHAR(10) NULL
);

CREATE TABLE products (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    sku VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    condition_status VARCHAR(30) NOT NULL,
    posted_at DATETIME(6) NOT NULL,
    zip_code VARCHAR(10) NOT NULL,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) REFERENCES categories(id),
    CONSTRAINT fk_products_zip FOREIGN KEY (zip_code) REFERENCES zip_reference(zip_code),
    INDEX idx_products_category (category_id),
    INDEX idx_products_price (price),
    INDEX idx_products_condition (condition_status),
    INDEX idx_products_posted_at (posted_at),
    INDEX idx_products_zip (zip_code)
);

CREATE TABLE search_query_events (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    query_text VARCHAR(255) NULL,
    category_name VARCHAR(100) NULL,
    min_price DECIMAL(10,2) NULL,
    max_price DECIMAL(10,2) NULL,
    condition_status VARCHAR(30) NULL,
    zip_code VARCHAR(10) NULL,
    distance_miles INT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    INDEX idx_search_query_events_username_created_at (username, created_at),
    INDEX idx_search_query_events_query_text (query_text),
    INDEX idx_search_query_events_created_at (created_at)
);

INSERT INTO categories (name) VALUES
('Electronics'),
('Home'),
('Fashion'),
('Sports'),
('Groceries');

INSERT INTO zip_reference (zip_code, latitude, longitude, city, state_code) VALUES
('10001', 40.750742, -73.996530, 'New York', 'NY'),
('30301', 33.752877, -84.392517, 'Atlanta', 'GA'),
('60601', 41.885310, -87.621849, 'Chicago', 'IL'),
('73301', 30.266666, -97.733330, 'Austin', 'TX'),
('85001', 33.448376, -112.074036, 'Phoenix', 'AZ'),
('90001', 33.973951, -118.248405, 'Los Angeles', 'CA'),
('94105', 37.789800, -122.394200, 'San Francisco', 'CA');

INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code)
SELECT 'ELEC-1001', '4K Retail Display Monitor', 'Demo display monitor for storefront analytics', c.id, 249.99, 'NEW', NOW() - INTERVAL 2 DAY, '10001'
FROM categories c WHERE c.name = 'Electronics';

INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code)
SELECT 'ELEC-1002', 'Handheld Barcode Scanner', 'High-speed scanner for checkout operations', c.id, 89.00, 'USED', NOW() - INTERVAL 1 DAY, '30301'
FROM categories c WHERE c.name = 'Electronics';

INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code)
SELECT 'HOME-2001', 'Backroom Storage Shelf', 'Industrial shelf for inventory room', c.id, 149.50, 'REFURBISHED', NOW() - INTERVAL 6 DAY, '60601'
FROM categories c WHERE c.name = 'Home';

INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code)
SELECT 'FASH-3001', 'Seasonal Display Rack', 'Display rack for apparel merchandising', c.id, 120.00, 'NEW', NOW() - INTERVAL 4 DAY, '90001'
FROM categories c WHERE c.name = 'Fashion';

INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code)
SELECT 'SPORT-4001', 'Point-of-Sale Tablet Stand', 'Adjustable stand for mobile POS station', c.id, 59.99, 'NEW', NOW() - INTERVAL 3 DAY, '73301'
FROM categories c WHERE c.name = 'Sports';

INSERT INTO products (sku, name, description, category_id, price, condition_status, posted_at, zip_code)
SELECT 'GROC-5001', 'Cold Storage Temperature Sensor', 'Sensor module for perishable inventory', c.id, 210.00, 'USED', NOW() - INTERVAL 8 DAY, '94105'
FROM categories c WHERE c.name = 'Groceries';
