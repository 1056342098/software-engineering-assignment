ALTER TABLE policy_doc
  ADD COLUMN version_label VARCHAR(32) NULL AFTER category,
  ADD COLUMN summary_text TEXT NULL AFTER file_path,
  ADD COLUMN standard_answer TEXT NULL AFTER summary_text;

CREATE TABLE notification (
  id BIGINT NOT NULL AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  attachment_name VARCHAR(255) NULL,
  attachment_url VARCHAR(1024) NULL,
  expire_at TIMESTAMP NULL,
  tags_json JSON NULL,
  channels_json JSON NOT NULL,
  target_json JSON NULL,
  created_by BIGINT NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY ix_notification_created_by (created_by),
  KEY ix_notification_expire_at (expire_at),
  CONSTRAINT fk_notification_created_by FOREIGN KEY (created_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_recipient (
  id BIGINT NOT NULL AUTO_INCREMENT,
  notification_id BIGINT NOT NULL,
  student_id BIGINT NOT NULL,
  delivery_status VARCHAR(24) NOT NULL,
  read_status VARCHAR(16) NOT NULL DEFAULT 'UNREAD',
  read_at TIMESTAMP NULL,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_recipient (notification_id, student_id),
  KEY ix_notification_recipient_student (student_id, read_status),
  CONSTRAINT fk_notification_recipient_notification FOREIGN KEY (notification_id) REFERENCES notification (id),
  CONSTRAINT fk_notification_recipient_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE notification_delivery (
  id BIGINT NOT NULL AUTO_INCREMENT,
  recipient_id BIGINT NOT NULL,
  channel VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  provider_message VARCHAR(255) NULL,
  sent_at TIMESTAMP NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_delivery_channel (recipient_id, channel),
  KEY ix_notification_delivery_status (channel, status),
  CONSTRAINT fk_notification_delivery_recipient FOREIGN KEY (recipient_id) REFERENCES notification_recipient (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
