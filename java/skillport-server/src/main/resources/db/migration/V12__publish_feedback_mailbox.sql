ALTER TABLE feedback_messages
    ADD COLUMN submitter_display_name VARCHAR(120) NULL AFTER owner_id;

UPDATE feedback_messages feedback
LEFT JOIN users account ON account.public_id = feedback.owner_id
SET feedback.submitter_display_name = COALESCE(NULLIF(TRIM(account.display_name), ''), 'SkillPort 用户')
WHERE feedback.submitter_display_name IS NULL;

ALTER TABLE feedback_messages
    MODIFY COLUMN submitter_display_name VARCHAR(120) NOT NULL;

CREATE INDEX idx_feedback_public_created
    ON feedback_messages (created_at, id);
