ALTER TABLE approval
  ADD COLUMN stage_code VARCHAR(32) NULL,
  ADD COLUMN stage_index INT NULL;

CREATE TABLE approval_process_progress (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  approval_type VARCHAR(32) NOT NULL,
  stage_index INT NOT NULL,
  stage_code VARCHAR(32) NOT NULL,
  last_result VARCHAR(16) NULL,
  last_assessed_at TIMESTAMP NULL,
  last_approval_id BIGINT NULL,
  next_due_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_process_progress (user_id, approval_type),
  KEY ix_approval_process_progress_user_due (user_id, next_due_at),
  CONSTRAINT fk_approval_process_progress_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
  CONSTRAINT fk_approval_process_progress_last_approval FOREIGN KEY (last_approval_id) REFERENCES approval (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
