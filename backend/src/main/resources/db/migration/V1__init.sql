CREATE TABLE sys_role (
  id BIGINT NOT NULL AUTO_INCREMENT,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  level TINYINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_role_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user (
  id BIGINT NOT NULL AUTO_INCREMENT,
  login_name VARCHAR(32) NOT NULL,
  real_name VARCHAR(32) NOT NULL,
  email VARCHAR(64) NULL,
  wechat_open_id VARCHAR(64) NULL,
  password_hash VARCHAR(120) NOT NULL,
  status SMALLINT NOT NULL DEFAULT 1,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_sys_user_login_name (login_name),
  KEY ix_sys_user_wechat_open_id (wechat_open_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE sys_user_role (
  user_id BIGINT NOT NULL,
  role_id BIGINT NOT NULL,
  PRIMARY KEY (user_id, role_id),
  CONSTRAINT fk_sys_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_sys_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student (
  id BIGINT NOT NULL,
  student_no VARCHAR(32) NULL,
  major VARCHAR(64) NULL,
  grade INT NULL,
  class_name VARCHAR(64) NULL,
  PRIMARY KEY (id),
  KEY ix_student_grade (grade),
  CONSTRAINT fk_student_user FOREIGN KEY (id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student_profile (
  id BIGINT NOT NULL AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  public_json JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_student_profile_student_id (student_id),
  CONSTRAINT fk_student_profile_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE student_sensitive (
  id BIGINT NOT NULL AUTO_INCREMENT,
  student_id BIGINT NOT NULL,
  id_card_no_enc VARCHAR(255) NULL,
  hukou_addr_enc VARCHAR(255) NULL,
  hometown_enc VARCHAR(255) NULL,
  tutor_enc VARCHAR(255) NULL,
  delay_info_enc TEXT NULL,
  updated_by BIGINT NULL,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_sensitive_student_id (student_id),
  CONSTRAINT fk_student_sensitive_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE policy_doc (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  category VARCHAR(64) NULL,
  file_name VARCHAR(255) NULL,
  file_path VARCHAR(1024) NULL,
  uploader_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_policy_doc_category (category),
  KEY ix_policy_doc_uploader (uploader_id),
  CONSTRAINT fk_policy_doc_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE policy_doc_chunk (
  id BIGINT NOT NULL AUTO_INCREMENT,
  doc_id BIGINT NOT NULL,
  chunk_no INT NOT NULL,
  chunk_text LONGTEXT NOT NULL,
  PRIMARY KEY (id),
  KEY ix_policy_doc_chunk_doc_id (doc_id),
  FULLTEXT KEY fx_policy_doc_chunk_text (chunk_text),
  CONSTRAINT fk_policy_doc_chunk_doc FOREIGN KEY (doc_id) REFERENCES policy_doc (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE approval (
  id BIGINT NOT NULL AUTO_INCREMENT,
  applicant_id BIGINT NOT NULL,
  type VARCHAR(32) NOT NULL,
  status VARCHAR(16) NOT NULL,
  current_step INT NOT NULL DEFAULT 0,
  approver_id BIGINT NULL,
  window_expire_at TIMESTAMP NULL,
  form_json JSON NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_approval_applicant_status (applicant_id, status),
  CONSTRAINT fk_approval_applicant FOREIGN KEY (applicant_id) REFERENCES sys_user (id),
  CONSTRAINT fk_approval_approver FOREIGN KEY (approver_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE approval_step (
  id BIGINT NOT NULL AUTO_INCREMENT,
  approval_id BIGINT NOT NULL,
  step_no INT NOT NULL,
  name VARCHAR(64) NOT NULL,
  status VARCHAR(16) NOT NULL,
  due_at TIMESTAMP NULL,
  acted_by BIGINT NULL,
  acted_at TIMESTAMP NULL,
  comment VARCHAR(255) NULL,
  PRIMARY KEY (id),
  KEY ix_approval_step_approval_no (approval_id, step_no),
  CONSTRAINT fk_approval_step_approval FOREIGN KEY (approval_id) REFERENCES approval (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE approval_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  approval_id BIGINT NOT NULL,
  operator_id BIGINT NOT NULL,
  action VARCHAR(32) NOT NULL,
  comment VARCHAR(255) NULL,
  op_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_approval_log_approval_time (approval_id, op_time),
  CONSTRAINT fk_approval_log_approval FOREIGN KEY (approval_id) REFERENCES approval (id),
  CONSTRAINT fk_approval_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE operation_log (
  id BIGINT NOT NULL AUTO_INCREMENT,
  operator_id BIGINT NOT NULL,
  action VARCHAR(64) NOT NULL,
  target_type VARCHAR(64) NULL,
  target_id BIGINT NULL,
  detail_json JSON NULL,
  ts TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_operation_log_operator_action_ts (operator_id, action, ts),
  CONSTRAINT fk_operation_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
