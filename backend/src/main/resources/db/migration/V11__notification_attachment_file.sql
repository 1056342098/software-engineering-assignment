ALTER TABLE notification
  ADD COLUMN attachment_file_path VARCHAR(1024) NULL AFTER attachment_url,
  ADD COLUMN attachment_mime_type VARCHAR(128) NULL AFTER attachment_file_path,
  ADD COLUMN attachment_file_size BIGINT NULL AFTER attachment_mime_type;
