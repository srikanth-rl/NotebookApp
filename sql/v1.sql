DROP SCHEMA IF EXISTS notebook_app;
CREATE SCHEMA notebook_app;
USE notebook_app;


DROP TABLE IF EXISTS user;
CREATE TABLE user (
  id bigint NOT NULL AUTO_INCREMENT,
  display_name varchar(255) NOT NULL,
  status varchar(50) DEFAULT 'PENDING' COMMENT 'PENDING | VERIFIED',
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
);


DROP TABLE IF EXISTS user_password;
CREATE TABLE user_password (
  user_id bigint NOT NULL,
  password_hash varchar(255) NOT NULL,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (user_id),
  CONSTRAINT fk_password_user
    FOREIGN KEY (user_id) REFERENCES user (id)
    ON DELETE CASCADE
);


DROP TABLE IF EXISTS user_emails;
CREATE TABLE user_emails (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  email varchar(255) NOT NULL,
  is_verified tinyint(1) DEFAULT 0,
  is_primary tinyint(1) DEFAULT 0,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY email (email),
  KEY fk_email_user (user_id),
  CONSTRAINT fk_email_user
    FOREIGN KEY (user_id) REFERENCES user (id)
    ON DELETE CASCADE
);


DROP TABLE IF EXISTS user_phones;
CREATE TABLE user_phones (
  id bigint NOT NULL AUTO_INCREMENT,
  user_id bigint NOT NULL,
  phone_number varchar(50) NOT NULL,
  is_verified tinyint(1) DEFAULT 0,
  is_primary tinyint(1) DEFAULT 0,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY phone_number (phone_number),
  KEY fk_phone_user (user_id),
  CONSTRAINT fk_phone_user
    FOREIGN KEY (user_id) REFERENCES user (id)
    ON DELETE CASCADE
);


DROP TABLE IF EXISTS notebooks;
CREATE TABLE notebooks (
  id bigint NOT NULL AUTO_INCREMENT,
  title varchar(255),
  owner_type varchar(50) NOT NULL COMMENT 'USER | GROUP | ORG',
  owner_id bigint NOT NULL,
  created_by bigint,
  updated_by bigint,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_notebook_owner (owner_type, owner_id),
  KEY fk_notebook_created_by (created_by),
  KEY fk_notebook_updated_by (updated_by),
  CONSTRAINT fk_notebook_created_by
    FOREIGN KEY (created_by) REFERENCES user (id)
    ON DELETE SET NULL,
  CONSTRAINT fk_notebook_updated_by
    FOREIGN KEY (updated_by) REFERENCES user (id)
    ON DELETE SET NULL
);


DROP TABLE IF EXISTS notes;
CREATE TABLE notes (
  id bigint NOT NULL AUTO_INCREMENT,
  notebook_id bigint NOT NULL,
  title varchar(255),
  content text,
  is_pinned tinyint(1) DEFAULT 0,
  created_by bigint,
  updated_by bigint,
  created_at timestamp DEFAULT CURRENT_TIMESTAMP,
  updated_at timestamp DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY fk_note_notebook (notebook_id),
  KEY fk_note_created_by (created_by),
  KEY fk_note_updated_by (updated_by),
  CONSTRAINT fk_note_notebook
    FOREIGN KEY (notebook_id) REFERENCES notebooks (id)
    ON DELETE CASCADE,
  CONSTRAINT fk_note_created_by
    FOREIGN KEY (created_by) REFERENCES user (id)
    ON DELETE SET NULL,
  CONSTRAINT fk_note_updated_by
    FOREIGN KEY (updated_by) REFERENCES user (id)
    ON DELETE SET NULL
);
