package com.schoolmanager.backend.notification;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-email")
public class NotificationEmailSettingsController {
	private final NotificationEmailSettingsService settingsService;
	private final CurrentUser currentUser;

	public NotificationEmailSettingsController(NotificationEmailSettingsService settingsService, CurrentUser currentUser) {
		this.settingsService = settingsService;
		this.currentUser = currentUser;
	}

	@GetMapping("/me")
	public ApiResponse<NotificationEmailSettingsService.MySettingsView> me() {
		return ApiResponse.ok(settingsService.getMySettings(currentUser.id(), currentUser.roleCodes()));
	}

	@PutMapping("/me/recipient")
	public ApiResponse<NotificationEmailSettingsService.MySettingsView> updateRecipient(@Valid @RequestBody RecipientReq req) {
		return ApiResponse.ok(settingsService.updateRecipientEmail(currentUser.id(), currentUser.roleCodes(), req.recipientEmail()));
	}

	@PutMapping("/me/sender")
	public ApiResponse<NotificationEmailSettingsService.MySettingsView> updateSender(@Valid @RequestBody SenderReq req) {
		return ApiResponse.ok(settingsService.upsertSenderSettings(
				currentUser.id(),
				currentUser.roleCodes(),
				new NotificationEmailSettingsService.UpdateSenderCommand(
						req.senderEmail(),
						req.senderName(),
						req.smtpHost(),
						req.smtpPort(),
						req.smtpUsername(),
						req.smtpPassword(),
						req.starttlsEnabled(),
						req.sslEnabled())));
	}

	public record RecipientReq(String recipientEmail) {
	}

	public record SenderReq(
			String senderEmail,
			String senderName,
			String smtpHost,
			@NotNull Integer smtpPort,
			String smtpUsername,
			String smtpPassword,
			boolean starttlsEnabled,
			boolean sslEnabled) {
	}
}
