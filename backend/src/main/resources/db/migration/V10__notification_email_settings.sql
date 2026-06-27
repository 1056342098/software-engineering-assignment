CREATE TABLE notification_email_config (
  id BIGINT NOT NULL AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  sender_email VARCHAR(128) NOT NULL,
  sender_name VARCHAR(64) NULL,
  smtp_host VARCHAR(255) NOT NULL,
  smtp_port INT NOT NULL,
  smtp_username VARCHAR(255) NOT NULL,
  smtp_password_enc VARCHAR(512) NOT NULL,
  starttls_enabled TINYINT(1) NOT NULL DEFAULT 1,
  ssl_enabled TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP NULL DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_notification_email_config_user_id (user_id),
  CONSTRAINT fk_notification_email_config_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
