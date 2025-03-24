CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(36) PRIMARY KEY,
    producer_id VARCHAR(255) NOT NULL,
    qr_code VARCHAR(255) NOT NULL,
    produced_at DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL
); 