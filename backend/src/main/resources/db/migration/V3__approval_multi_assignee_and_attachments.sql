ALTER TABLE approval
  ADD COLUMN subject VARCHAR(255) NULL,
  ADD COLUMN content LONGTEXT NULL;

CREATE TABLE approval_assignee (
  id BIGINT NOT NULL AUTO_INCREMENT,
  approval_id BIGINT NOT NULL,
  approver_id BIGINT NOT NULL,
  status VARCHAR(16) NOT NULL,
  acted_at TIMESTAMP NULL,
  comment VARCHAR(255) NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_approval_assignee (approval_id, approver_id),
  KEY ix_approval_assignee_approver_status (approver_id, status),
  CONSTRAINT fk_approval_assignee_approval FOREIGN KEY (approval_id) REFERENCES approval (id),
  CONSTRAINT fk_approval_assignee_approver FOREIGN KEY (approver_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO approval_assignee (approval_id, approver_id, status)
SELECT a.id,
       a.approver_id,
       CASE
         WHEN a.status = 'APPROVED' THEN 'APPROVED'
         WHEN a.status = 'REJECTED' THEN 'REJECTED'
         ELSE 'PENDING'
       END AS status
FROM approval a
WHERE a.approver_id IS NOT NULL
  AND NOT EXISTS (
    SELECT 1 FROM approval_assignee aa
    WHERE aa.approval_id = a.id AND aa.approver_id = a.approver_id
  );

CREATE TABLE approval_attachment (
  id BIGINT NOT NULL AUTO_INCREMENT,
  approval_id BIGINT NOT NULL,
  uploader_id BIGINT NOT NULL,
  file_name VARCHAR(255) NOT NULL,
  file_path VARCHAR(1024) NOT NULL,
  mime_type VARCHAR(128) NULL,
  file_size BIGINT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_approval_attachment_approval (approval_id, id),
  CONSTRAINT fk_approval_attachment_approval FOREIGN KEY (approval_id) REFERENCES approval (id),
  CONSTRAINT fk_approval_attachment_uploader FOREIGN KEY (uploader_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
