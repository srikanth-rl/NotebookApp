USE notebook_app;

ALTER TABLE user 
DROP COLUMN session_timeout_minutes;

ALTER TABLE user 
ADD COLUMN session_timeout_minutes INT NOT NULL DEFAULT 30 COMMENT 'Timeout in minutes';

COMMIT;