USE notebook_app;

ALTER TABLE notebooks ADD COLUMN created_at_ms BIGINT, ADD COLUMN updated_at_ms BIGINT;
ALTER TABLE notes ADD COLUMN created_at_ms BIGINT, ADD COLUMN updated_at_ms BIGINT;
ALTER TABLE user ADD COLUMN created_at_ms BIGINT, ADD COLUMN updated_at_ms BIGINT;
ALTER TABLE user_emails ADD COLUMN created_at_ms BIGINT, ADD COLUMN updated_at_ms BIGINT;
ALTER TABLE user_password ADD COLUMN created_at_ms BIGINT, ADD COLUMN updated_at_ms BIGINT;
ALTER TABLE user_phones ADD COLUMN created_at_ms BIGINT, ADD COLUMN updated_at_ms BIGINT;
ALTER TABLE user_sessions ADD COLUMN last_accessed_at_ms BIGINT;

UPDATE notebooks SET created_at_ms = ROUND(UNIX_TIMESTAMP(created_at) * 1000), updated_at_ms = ROUND(UNIX_TIMESTAMP(updated_at) * 1000);
UPDATE notes SET created_at_ms = ROUND(UNIX_TIMESTAMP(created_at) * 1000), updated_at_ms = ROUND(UNIX_TIMESTAMP(updated_at) * 1000);
UPDATE user SET created_at_ms = ROUND(UNIX_TIMESTAMP(created_at) * 1000), updated_at_ms = ROUND(UNIX_TIMESTAMP(updated_at) * 1000);
UPDATE user_emails SET created_at_ms = ROUND(UNIX_TIMESTAMP(created_at) * 1000), updated_at_ms = ROUND(UNIX_TIMESTAMP(updated_at) * 1000);
UPDATE user_password SET created_at_ms = ROUND(UNIX_TIMESTAMP(created_at) * 1000), updated_at_ms = ROUND(UNIX_TIMESTAMP(updated_at) * 1000);
UPDATE user_phones SET created_at_ms = ROUND(UNIX_TIMESTAMP(created_at) * 1000), updated_at_ms = ROUND(UNIX_TIMESTAMP(updated_at) * 1000);
UPDATE user_sessions SET last_accessed_at_ms = ROUND(UNIX_TIMESTAMP(last_accessed_at) * 1000);

ALTER TABLE notebooks DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE notebooks RENAME COLUMN created_at_ms TO created_at, RENAME COLUMN updated_at_ms TO updated_at;

ALTER TABLE notes DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE notes RENAME COLUMN created_at_ms TO created_at, RENAME COLUMN updated_at_ms TO updated_at;

ALTER TABLE user DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE user RENAME COLUMN created_at_ms TO created_at, RENAME COLUMN updated_at_ms TO updated_at;

ALTER TABLE user_emails DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE user_emails RENAME COLUMN created_at_ms TO created_at, RENAME COLUMN updated_at_ms TO updated_at;

ALTER TABLE user_password DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE user_password RENAME COLUMN created_at_ms TO created_at, RENAME COLUMN updated_at_ms TO updated_at;

ALTER TABLE user_phones DROP COLUMN created_at, DROP COLUMN updated_at;
ALTER TABLE user_phones RENAME COLUMN created_at_ms TO created_at, RENAME COLUMN updated_at_ms TO updated_at;

ALTER TABLE user_sessions DROP COLUMN last_accessed_at;
ALTER TABLE user_sessions RENAME COLUMN last_accessed_at_ms TO last_accessed_at;

COMMIT;