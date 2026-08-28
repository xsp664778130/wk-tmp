CREATE TABLE device_local_skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id VARCHAR(128) NOT NULL,
    device_public_id VARCHAR(36) NOT NULL,
    tool VARCHAR(32) NOT NULL,
    slug VARCHAR(180) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(1000) NOT NULL,
    relative_path VARCHAR(512) NOT NULL,
    origin_skill_id VARCHAR(64) NULL,
    detected_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_device_local_skill (device_public_id, tool, slug),
    KEY idx_local_skills_owner_device (owner_id, device_public_id),
    CONSTRAINT fk_local_skills_device
        FOREIGN KEY (device_public_id) REFERENCES devices(public_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
