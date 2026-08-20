CREATE TABLE skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path VARCHAR(512) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    note VARCHAR(2000) NOT NULL DEFAULT '',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_skills_public_id (public_id),
    KEY idx_skills_owner_created (owner_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE devices (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    name VARCHAR(120) NOT NULL,
    os VARCHAR(32) NOT NULL,
    arch VARCHAR(32) NOT NULL,
    token_hash CHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    last_seen_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_devices_public_id (public_id),
    KEY idx_devices_owner_seen (owner_id, last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE pairing_codes (
    code_hash CHAR(64) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    consumed_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (code_hash),
    KEY idx_pairing_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE install_tasks (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    skill_public_id VARCHAR(36) NOT NULL,
    device_public_id VARCHAR(36) NOT NULL,
    targets VARCHAR(255) NOT NULL,
    status VARCHAR(24) NOT NULL,
    progress INT NOT NULL,
    stage VARCHAR(64) NOT NULL,
    error_message VARCHAR(1000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_tasks_public_id (public_id),
    KEY idx_tasks_owner_created (owner_id, created_at),
    KEY idx_tasks_device_status (device_public_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE download_tickets (
    token_hash CHAR(64) NOT NULL,
    owner_id VARCHAR(128) NOT NULL,
    skill_public_id VARCHAR(36) NOT NULL,
    device_public_id VARCHAR(36) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (token_hash),
    KEY idx_download_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
