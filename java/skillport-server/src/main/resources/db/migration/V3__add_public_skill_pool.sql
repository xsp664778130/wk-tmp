ALTER TABLE skills
    ADD COLUMN source_public_skill_id VARCHAR(36) NULL AFTER note,
    ADD UNIQUE KEY uk_skills_owner_source (owner_id, source_public_skill_id);

CREATE TABLE public_skills (
    id BIGINT NOT NULL AUTO_INCREMENT,
    public_id VARCHAR(36) NOT NULL,
    source_skill_public_id VARCHAR(36) NOT NULL,
    publisher_owner_id VARCHAR(128) NOT NULL,
    publisher_display_name VARCHAR(120) NOT NULL,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    category VARCHAR(64) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(120) NOT NULL,
    size_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    tool_compatibility VARCHAR(120) NOT NULL,
    pull_count BIGINT NOT NULL DEFAULT 0,
    published_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_public_skills_public_id (public_id),
    UNIQUE KEY uk_public_skills_source (source_skill_public_id),
    KEY idx_public_skills_published (published_at),
    KEY idx_public_skills_publisher (publisher_owner_id, published_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
