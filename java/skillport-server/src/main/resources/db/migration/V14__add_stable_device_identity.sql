ALTER TABLE devices
    ADD COLUMN client_instance_id VARCHAR(64) NULL AFTER owner_id;

CREATE UNIQUE INDEX uk_devices_owner_instance
    ON devices (owner_id, client_instance_id);
