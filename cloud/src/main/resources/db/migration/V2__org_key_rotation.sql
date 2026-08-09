-- org-key 로테이션: 직전 키 유예기간 + 감사 컬럼. 무중단 키 교체 지원.
ALTER TABLE organization ADD COLUMN previous_api_key character varying(255);
ALTER TABLE organization ADD COLUMN previous_key_expires_at timestamp(6) without time zone;
ALTER TABLE organization ADD COLUMN key_rotated_at timestamp(6) without time zone;
