USE notebook_app;
DROP TABLE IF EXISTS user_sessions;

CREATE TABLE user_sessions (
  id BIGINT NOT NULL AUTO_INCREMENT,
  session_id VARCHAR(255) NOT NULL,
  user_id BIGINT NOT NULL,
  
  ip_address VARCHAR(45),
  user_agent TEXT,
  
  created_at BIGINT,
  expires_at BIGINT,

  PRIMARY KEY (id),

  UNIQUE KEY uk_session_id (session_id),

  KEY idx_user_id (user_id),
  KEY idx_expires_at (expires_at),

  CONSTRAINT fk_user_session_user
    FOREIGN KEY (user_id)
    REFERENCES user (id)
    ON DELETE CASCADE
);

DROP TABLE IF EXISTS system_configs;

CREATE TABLE system_configs (
  id bigint NOT NULL AUTO_INCREMENT,
  
  config_key varchar(255) NOT NULL COMMENT 'session_timeout_minutes, max_concurrent_sessions, sharing_enabled, max_notebooks..',
  config_value text NOT NULL COMMENT 'Stored as TEXT to support numeric / boolean,..',
  config_type varchar(50) NOT NULL DEFAULT 'STRING'  COMMENT 'Data type of the key - INT | BOOLEAN | STRING',
  
  description varchar(500) DEFAULT NULL COMMENT 'Config purpose',
  
  created_at bigint,
  updated_at bigint,
  
  PRIMARY KEY (id),
  UNIQUE KEY uk_system_config_key (config_key)
);

COMMIT;