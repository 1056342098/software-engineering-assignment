package com.schoolmanager.backend.notification;

import java.util.List;

public interface NotificationEmailGateway {
	String send(SendCommand command);

	record SendCommand(
			String senderEmail,
			String senderName,
			String smtpHost,
			int smtpPort,
			String smtpUsername,
			String smtpPassword,
			boolean starttlsEnabled,
			boolean sslEnabled,
			String toEmail,
			String subject,
			String text,
			List<Attachment> attachments) {
	}

	record Attachment(
			String fileName,
			String filePath,
			String mimeType) {
	}
}
