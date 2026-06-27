package com.schoolmanager.backend.notification;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Component
public class SmtpNotificationEmailGateway implements NotificationEmailGateway {
	@Override
	public String send(SendCommand command) {
		JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
		mailSender.setHost(command.smtpHost());
		mailSender.setPort(command.smtpPort());
		mailSender.setUsername(command.smtpUsername());
		mailSender.setPassword(command.smtpPassword());
		mailSender.setDefaultEncoding(StandardCharsets.UTF_8.name());

		Properties props = mailSender.getJavaMailProperties();
		props.setProperty("mail.transport.protocol", "smtp");
		props.setProperty("mail.smtp.auth", "true");
		props.setProperty("mail.smtp.starttls.enable", String.valueOf(command.starttlsEnabled()));
		props.setProperty("mail.smtp.ssl.enable", String.valueOf(command.sslEnabled()));
		props.setProperty("mail.smtp.connectiontimeout", "10000");
		props.setProperty("mail.smtp.timeout", "10000");
		props.setProperty("mail.smtp.writetimeout", "10000");

		MimeMessage mimeMessage = mailSender.createMimeMessage();
		try {
			boolean hasAttachments = command.attachments() != null && !command.attachments().isEmpty();
			MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, hasAttachments, StandardCharsets.UTF_8.name());
			if (command.senderName() == null || command.senderName().isBlank()) {
				helper.setFrom(command.senderEmail());
			} else {
				helper.setFrom(new InternetAddress(command.senderEmail(), command.senderName(),
						StandardCharsets.UTF_8.name()));
			}
			helper.setTo(command.toEmail());
			helper.setSubject(command.subject());
			helper.setText(command.text(), false);
			if (hasAttachments) {
				for (NotificationEmailGateway.Attachment attachment : command.attachments()) {
					helper.addAttachment(attachment.fileName(), new FileSystemResource(attachment.filePath()), attachment.mimeType());
				}
			}
			mailSender.send(mimeMessage);
			return "SMTP 已发送";
		} catch (Exception e) {
			throw new IllegalStateException("SMTP_SEND_FAILED: " + e.getMessage(), e);
		}
	}
}
