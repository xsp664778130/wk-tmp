ALTER TABLE devices
    ADD COLUMN installed_tools VARCHAR(255) NOT NULL DEFAULT '' AFTER status,
    ADD COLUMN tools_detected_at TIMESTAMP(6) NULL AFTER installed_tools;
