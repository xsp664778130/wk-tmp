CREATE TABLE feedback_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    kind VARCHAR(32) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_feedback_public_id (public_id),
    KEY idx_feedback_owner_created (owner_id, created_at),
    KEY idx_feedback_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
