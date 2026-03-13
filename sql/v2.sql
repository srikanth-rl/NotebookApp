USE notebook_app;

ALTER TABLE user
MODIFY created_at DATETIME NOT NULL,
MODIFY updated_at DATETIME NOT NULL;

ALTER TABLE user_emails
MODIFY created_at DATETIME NOT NULL,
MODIFY updated_at DATETIME NOT NULL;

ALTER TABLE user_phones
MODIFY created_at DATETIME NOT NULL,
MODIFY updated_at DATETIME NOT NULL;

ALTER TABLE user_phones
MODIFY phone_number VARCHAR(50) NULL;