-- 기본 역할 생성
INSERT INTO roles (name, description) VALUES ('ROLE_ADMIN', '시스템 관리자') ON DUPLICATE KEY UPDATE description = '시스템 관리자';
INSERT INTO roles (name, description) VALUES ('ROLE_USER', '일반 사용자') ON DUPLICATE KEY UPDATE description = '일반 사용자';
INSERT INTO roles (name, description) VALUES ('ROLE_MANAGER', '부서 관리자') ON DUPLICATE KEY UPDATE description = '부서 관리자'; 