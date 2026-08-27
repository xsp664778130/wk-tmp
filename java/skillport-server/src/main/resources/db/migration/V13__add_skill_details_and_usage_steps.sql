ALTER TABLE skills
    ADD COLUMN detail_text TEXT NULL AFTER description,
    ADD COLUMN usage_steps TEXT NULL AFTER detail_text;

UPDATE skills
SET detail_text = description,
    usage_steps = ''
WHERE detail_text IS NULL OR usage_steps IS NULL;

ALTER TABLE skills
    MODIFY COLUMN detail_text TEXT NOT NULL,
    MODIFY COLUMN usage_steps TEXT NOT NULL;

ALTER TABLE public_skills
    ADD COLUMN detail_text TEXT NULL AFTER description,
    ADD COLUMN usage_steps TEXT NULL AFTER detail_text;

UPDATE public_skills
SET detail_text = description,
    usage_steps = ''
WHERE detail_text IS NULL OR usage_steps IS NULL;

ALTER TABLE public_skills
    MODIFY COLUMN detail_text TEXT NOT NULL,
    MODIFY COLUMN usage_steps TEXT NOT NULL;
