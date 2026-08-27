ALTER TABLE skills
    ADD COLUMN avatar_file_name VARCHAR(255) NULL AFTER sha256,
    ADD COLUMN avatar_storage_path VARCHAR(512) NULL AFTER avatar_file_name,
    ADD COLUMN avatar_content_type VARCHAR(120) NULL AFTER avatar_storage_path,
    ADD COLUMN avatar_size_bytes BIGINT NULL AFTER avatar_content_type,
    ADD COLUMN avatar_sha256 CHAR(64) NULL AFTER avatar_size_bytes;
