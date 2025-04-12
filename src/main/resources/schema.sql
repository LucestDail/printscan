-- 사용자 역할 테이블
CREATE TABLE IF NOT EXISTS roles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- 사용자 테이블
CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(20) NOT NULL,
    department VARCHAR(100) NOT NULL,
    position VARCHAR(100),
    profile_image_url VARCHAR(255) DEFAULT '/images/default-profile.png',
    created_at DATETIME,
    updated_at DATETIME,
    status VARCHAR(20) DEFAULT 'ACTIVE',
    last_login_at DATETIME,
    failed_login_attempts INT DEFAULT 0,
    enabled BOOLEAN DEFAULT TRUE
);

-- 사용자-역할 연결 테이블
CREATE TABLE IF NOT EXISTS user_roles (
    user_id BIGINT,
    role_id BIGINT,
    PRIMARY KEY (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- 제품 테이블
CREATE TABLE IF NOT EXISTS products (
    id VARCHAR(36) PRIMARY KEY,
    producer_id VARCHAR(255) NOT NULL,
    qr_code VARCHAR(255) NOT NULL,
    produced_at DATETIME NOT NULL,
    status VARCHAR(50) NOT NULL
); 