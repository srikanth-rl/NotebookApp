USE notebook_app;

DROP TABLE IF EXISTS audit_logs;

CREATE TABLE audit_logs (
  id BIGINT NOT NULL AUTO_INCREMENT,

  user_id BIGINT NOT NULL COMMENT 'Actor who performed the action',

  target_table VARCHAR(100) NOT NULL COMMENT 'Table on which action was performed',

  target_id BIGINT NOT NULL COMMENT 'Affected record id',

  action VARCHAR(50) NOT NULL COMMENT 'CREATE, UPDATE, DELETE',

  old_data TEXT COMMENT 'Previous state of the record in TEXT/JSON',

  new_data TEXT COMMENT 'New state of the record in TEXT/JSON' AFTER old_data,

  ip_address VARCHAR(100),

  created_at BIGINT,

  PRIMARY KEY (id),

  INDEX idx_user_id (user_id),
  INDEX idx_target_table (target_table),
  INDEX idx_action (action),

  CONSTRAINT fk_audit_user
    FOREIGN KEY (user_id)
    REFERENCES user(id)
    ON DELETE NO ACTION
    ON UPDATE NO ACTION
);

COMMIT;