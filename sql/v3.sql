USE notebook_app;

ALTER TABLE user
ADD COLUMN timezone VARCHAR(255) NOT NULL DEFAULT 'Asia/Kolkata';

ALTER TABLE user_phones
DROP INDEX phone_number;

ALTER TABLE user_phones
DROP COLUMN phone_number;

ALTER TABLE user_phones
ADD COLUMN phone_number VARCHAR(50) NOT NULL;

CREATE INDEX idx_phone_lookup
ON user_phones (phone_number, is_primary);