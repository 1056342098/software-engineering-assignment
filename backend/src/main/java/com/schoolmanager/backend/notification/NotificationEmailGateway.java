package com.schoolmanager.backend.notification;

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
			String text) {
	}
}
