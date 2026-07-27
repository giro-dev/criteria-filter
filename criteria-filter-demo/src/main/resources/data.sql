-- ============================================================
-- Sample data for criteria-filter demo
-- Run with: psql -U demo -d criteriafilter -f data.sql
-- Or let Spring Boot execute it automatically on startup
-- ============================================================

-- Brands
INSERT INTO brands (id, name, description, country, founded_year, active, created_at) VALUES
(1, 'Apple', 'Technology company', 'USA', 1976, true, NOW()),
(2, 'Samsung', 'Electronics conglomerate', 'South Korea', 1938, true, NOW()),
(3, 'Sony', 'Entertainment and electronics', 'Japan', 1946, true, NOW()),
(4, 'Nike', 'Athletic footwear and apparel', 'USA', 1964, true, NOW()),
(5, 'Adidas', 'Sportswear manufacturer', 'Germany', 1949, true, NOW()),
(6, 'OldBrand', 'Discontinued brand', 'UK', 1900, false, NOW());

-- Restart sequence
SELECT setval('brands_id_seq', (SELECT MAX(id) FROM brands));

-- Products
INSERT INTO products (id, name, description, price, category, active, stock, created_at, brand_id) VALUES
(1, 'iPhone 15 Pro', 'Latest Apple smartphone', 1199.99, 'Electronics', true, 50, NOW(), 1),
(2, 'iPhone 14', 'Previous gen Apple smartphone', 799.99, 'Electronics', true, 100, NOW(), 1),
(3, 'MacBook Pro 16', 'Professional laptop', 2499.99, 'Electronics', true, 25, NOW(), 1),
(4, 'AirPods Pro', 'Wireless earbuds', 249.99, 'Electronics', true, 200, NOW(), 1),
(5, 'Galaxy S24 Ultra', 'Samsung flagship phone', 1299.99, 'Electronics', true, 75, NOW(), 2),
(6, 'Galaxy Tab S9', 'Premium Android tablet', 849.99, 'Electronics', true, 40, NOW(), 2),
(7, 'Samsung TV 65"', '4K OLED Television', 1599.99, 'Electronics', true, 15, NOW(), 2),
(8, 'PlayStation 5', 'Gaming console', 499.99, 'Gaming', true, 30, NOW(), 3),
(9, 'Sony WH-1000XM5', 'Noise cancelling headphones', 399.99, 'Electronics', true, 60, NOW(), 3),
(10, 'Sony Camera A7IV', 'Mirrorless camera', 2499.99, 'Electronics', true, 10, NOW(), 3),
(11, 'Air Jordan 1', 'Classic basketball shoes', 180.00, 'Footwear', true, 150, NOW(), 4),
(12, 'Nike Air Max', 'Running shoes', 150.00, 'Footwear', true, 200, NOW(), 4),
(13, 'Nike Dri-FIT Shirt', 'Athletic t-shirt', 35.00, 'Apparel', true, 500, NOW(), 4),
(14, 'Adidas Ultraboost', 'Premium running shoes', 190.00, 'Footwear', true, 120, NOW(), 5),
(15, 'Adidas Originals Hoodie', 'Classic hoodie', 80.00, 'Apparel', true, 300, NOW(), 5),
(16, 'Old iPhone', 'Discontinued model', 299.99, 'Electronics', false, 0, NOW(), 1),
(17, 'Old Galaxy', 'Discontinued model', 199.99, 'Electronics', false, 0, NOW(), 2);

SELECT setval('products_id_seq', (SELECT MAX(id) FROM products));

-- Orders
INSERT INTO orders (id, order_number, customer_name, customer_email, status, total_amount, item_count, notes, created_at, shipped_at, delivered_at) VALUES
(1, 'ORD-001', 'John Doe', 'john@example.com', 'DELIVERED', 1449.98, 2, 'First order', NOW() - INTERVAL '5 days', NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days'),
(2, 'ORD-002', 'Jane Smith', 'jane@example.com', 'SHIPPED', 2499.99, 1, 'Premium delivery', NOW() - INTERVAL '3 days', NOW() - INTERVAL '1 day', NULL),
(3, 'ORD-003', 'Bob Wilson', 'bob@example.com', 'CONFIRMED', 499.99, 1, NULL, NOW() - INTERVAL '1 day', NULL, NULL),
(4, 'ORD-004', 'Alice Brown', 'alice@example.com', 'PENDING', 180.00, 1, 'Waiting payment', NOW(), NULL, NULL),
(5, 'ORD-005', 'Charlie Davis', 'charlie@example.com', 'DELIVERED', 3699.98, 3, 'Large order', NOW() - INTERVAL '7 days', NOW() - INTERVAL '6 days', NOW() - INTERVAL '4 days'),
(6, 'ORD-006', 'Diana Miller', 'diana@example.com', 'CANCELLED', 849.99, 1, 'Customer cancelled', NOW() - INTERVAL '2 days', NULL, NULL),
(7, 'ORD-007', 'Eve Johnson', 'eve@example.com', 'SHIPPED', 1599.99, 1, NULL, NOW() - INTERVAL '4 days', NOW() - INTERVAL '2 days', NULL),
(8, 'ORD-008', 'Frank Lee', 'frank@example.com', 'PENDING', 429.99, 2, 'Express shipping requested', NOW() - INTERVAL '1 day', NULL, NULL),
(9, 'ORD-009', 'Grace Kim', 'grace@example.com', 'CONFIRMED', 2749.99, 2, NULL, NOW() - INTERVAL '2 days', NULL, NULL),
(10, 'ORD-010', 'Henry Chen', 'henry@example.com', 'DELIVERED', 115.00, 3, NULL, NOW() - INTERVAL '6 days', NOW() - INTERVAL '5 days', NOW() - INTERVAL '3 days');

SELECT setval('orders_id_seq', (SELECT MAX(id) FROM orders));

-- Customers with JSONB fields
INSERT INTO customers (id, email, first_name, last_name, country, active, loyalty_points, preferences, metadata, address, created_at, last_login_at) VALUES
(1, 'john.doe@example.com', 'John', 'Doe', 'USA', true, 5000,
 '{"theme": "dark", "language": "en", "notifications": {"email": true, "sms": false}}'::jsonb,
 '{"source": "web", "campaign": "summer2024", "tags": ["vip", "early-adopter"]}'::jsonb,
 '{"street": "123 Main St", "city": "New York", "zip": "10001", "country": "USA"}'::jsonb,
 NOW(), NULL),

(2, 'jane.smith@example.com', 'Jane', 'Smith', 'UK', true, 1500,
 '{"theme": "light", "language": "en", "notifications": {"email": true, "sms": true}}'::jsonb,
 '{"source": "mobile", "campaign": "winter2023", "tags": ["regular"]}'::jsonb,
 '{"street": "456 High St", "city": "London", "zip": "SW1A 1AA", "country": "UK"}'::jsonb,
 NOW(), NOW() - INTERVAL '1 day'),

(3, 'carlos.garcia@example.com', 'Carlos', 'Garcia', 'Spain', true, 8500,
 '{"theme": "system", "language": "es", "notifications": {"email": false, "sms": true}}'::jsonb,
 '{"source": "referral", "campaign": "spring2024", "tags": ["vip", "premium", "influencer"], "referredBy": "john.doe@example.com"}'::jsonb,
 '{"street": "Calle Mayor 789", "city": "Madrid", "zip": "28001", "country": "Spain"}'::jsonb,
 NOW(), NOW() - INTERVAL '2 days'),

(4, 'marie.dupont@example.com', 'Marie', 'Dupont', 'France', true, 100,
 '{"theme": "dark", "language": "fr"}'::jsonb,
 '{"source": "web", "tags": ["new"]}'::jsonb,
 NULL::jsonb,
 NOW(), NULL),

(5, 'old.user@example.com', 'Old', 'User', 'Germany', false, 0,
 '{"theme": "light", "language": "de"}'::jsonb,
 '{"source": "web", "tags": ["churned"], "deactivatedReason": "inactive"}'::jsonb,
 NULL::jsonb,
 NOW(), NOW() - INTERVAL '90 days'),

(6, 'yuki.tanaka@example.com', 'Yuki', 'Tanaka', 'Japan', true, 3200,
 '{"theme": "dark", "language": "ja", "notifications": {"email": true, "sms": true, "push": true}}'::jsonb,
 '{"source": "mobile", "campaign": "asia2024", "tags": ["vip", "mobile-first"]}'::jsonb,
 '{"street": "1-2-3 Shibuya", "city": "Tokyo", "zip": "150-0002", "country": "Japan"}'::jsonb,
 NOW(), NOW() - INTERVAL '3 days');

SELECT setval('customers_id_seq', (SELECT MAX(id) FROM customers));
