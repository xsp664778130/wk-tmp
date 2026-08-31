CREATE TABLE password_reset_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    email_normalized VARCHAR(254) NOT NULL,
    code_hash CHAR(64) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_public_id (public_id),
    KEY idx_password_reset_email_created (email_normalized, created_at),
    KEY idx_password_reset_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
