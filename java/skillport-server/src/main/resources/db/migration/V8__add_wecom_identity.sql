ALTER TABLE users ADD COLUMN wecom_corp_id VARCHAR(64) NULL;
ALTER TABLE users ADD COLUMN wecom_user_id VARCHAR(128) NULL;
CREATE UNIQUE INDEX uk_users_wecom_identity ON users (wecom_corp_id, wecom_user_id);
