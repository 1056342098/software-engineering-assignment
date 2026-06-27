package com.schoolmanager.backend.notification;

import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.crypto.AesCryptoService;
import com.schoolmanager.backend.notification.entity.NotificationEmailConfig;
import com.schoolmanager.backend.notification.repo.NotificationEmailConfigRepository;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import jakarta.mail.internet.InternetAddress;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class NotificationEmailSettingsService {
	private static final Set<String> SENDER_ROLES = Set.of("TEACHER", "LEADER", "CADRE");

	private final SysUserRepository userRepository;
	private final NotificationEmailConfigRepository configRepository;
	private final AesCryptoService cryptoService;
	private final OperationLogService opLogService;

	public NotificationEmailSettingsService(
			SysUserRepository userRepository,
			NotificationEmailConfigRepository configRepository,
			AesCryptoService cryptoService,
			OperationLogService opLogService) {
		this.userRepository = userRepository;
		this.configRepository = configRepository;
		this.cryptoService = cryptoService;
		this.opLogService = opLogService;
	}

	@Transactional(readOnly = true)
	public MySettingsView getMySettings(long userId, Set<String> roleCodes) {
		SysUser user = userRepository.findById(userId).orElseThrow(() -> new ApiException(404, "未找到用户"));
		NotificationEmailConfig config = configRepository.findByUser_Id(userId).orElse(null);
		boolean senderAvailable = canConfigureSender(roleCodes);
		return new MySettingsView(
				normalize(user.getEmail()),
				new SenderSettingsView(
						senderAvailable,
						config != null,
						config == null ? null : config.getSenderEmail(),
						config == null ? null : config.getSenderName(),
						config == null ? null : config.getSmtpHost(),
						config == null ? null : config.getSmtpPort(),
						config == null ? null : config.getSmtpUsername(),
						config != null && config.getSmtpPasswordEnc() != null && !config.getSmtpPasswordEnc().isBlank(),
						config != null && config.isStarttlsEnabled(),
						config != null && config.isSslEnabled()));
	}

	@Transactional
	public MySettingsView updateRecipientEmail(long userId, Set<String> roleCodes, String email) {
		SysUser user = userRepository.findById(userId).orElseThrow(() -> new ApiException(404, "未找到用户"));
		String normalized = normalize(email);
		if (normalized != null) {
			validateEmail(normalized, "收件邮箱格式无效");
		}
		user.setEmail(normalized);
		userRepository.save(user);
		opLogService.log(userId, "NOTIFICATION_RECIPIENT_EMAIL_UPDATE", "sys_user", userId, null);
		return getMySettings(userId, roleCodes);
	}

	@Transactional
	public MySettingsView upsertSenderSettings(long userId, Set<String> roleCodes, UpdateSenderCommand command) {
		if (!canConfigureSender(roleCodes)) {
			throw new ApiException(403, "当前角色没有配置发件邮箱的权限");
		}
		SysUser user = userRepository.findById(userId).orElseThrow(() -> new ApiException(404, "未找到用户"));
		NotificationEmailConfig config = configRepository.findByUser_Id(userId).orElseGet(NotificationEmailConfig::new);

		String senderEmail = requireEmail(command.senderEmail(), "发件邮箱不能为空");
		String senderName = normalize(command.senderName());
		String smtpHost = requireText(command.smtpHost(), "SMTP 主机不能为空");
		String smtpUsername = requireText(command.smtpUsername(), "SMTP 用户名不能为空");
		Integer smtpPort = command.smtpPort();
		if (smtpPort == null || smtpPort <= 0 || smtpPort > 65535) {
			throw new ApiException(400, "SMTP 端口无效");
		}

		config.setUser(user);
		config.setSenderEmail(senderEmail);
		config.setSenderName(senderName);
		config.setSmtpHost(smtpHost);
		config.setSmtpPort(smtpPort);
		config.setSmtpUsername(smtpUsername);
		config.setStarttlsEnabled(command.starttlsEnabled());
		config.setSslEnabled(command.sslEnabled());

		String rawPassword = normalize(command.smtpPassword());
		if (rawPassword != null) {
			config.setSmtpPasswordEnc(cryptoService.encryptToBase64(rawPassword));
		} else if (config.getSmtpPasswordEnc() == null || config.getSmtpPasswordEnc().isBlank()) {
			throw new ApiException(400, "SMTP 密码不能为空");
		}

		configRepository.save(config);
		opLogService.log(userId, "NOTIFICATION_SENDER_EMAIL_CONFIG_UPSERT", "notification_email_config", config.getId(), null);
		return getMySettings(userId, roleCodes);
	}

	@Transactional(readOnly = true)
	public ResolvedSenderConfig requireResolvedSender(long userId) {
		NotificationEmailConfig config = configRepository.findByUser_Id(userId)
				.orElseThrow(() -> new ApiException(400, "已选择邮件渠道，请先在个人资料中配置发件邮箱和 SMTP 信息"));
		return new ResolvedSenderConfig(
				config.getSenderEmail(),
				config.getSenderName(),
				config.getSmtpHost(),
				config.getSmtpPort(),
				config.getSmtpUsername(),
				cryptoService.decryptFromBase64(config.getSmtpPasswordEnc()),
				config.isStarttlsEnabled(),
				config.isSslEnabled());
	}

	private static boolean canConfigureSender(Set<String> roleCodes) {
		return roleCodes.stream().anyMatch(SENDER_ROLES::contains);
	}

	private static String requireText(String value, String message) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new ApiException(400, message);
		}
		return normalized;
	}

	private static String requireEmail(String value, String message) {
		String normalized = requireText(value, message);
		validateEmail(normalized, "邮箱格式无效");
		return normalized;
	}

	private static void validateEmail(String email, String message) {
		try {
			InternetAddress address = new InternetAddress(email, true);
			address.validate();
		} catch (Exception e) {
			throw new ApiException(400, message);
		}
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.isBlank() ? null : trimmed;
	}

	public record UpdateSenderCommand(
			String senderEmail,
			String senderName,
			String smtpHost,
			Integer smtpPort,
			String smtpUsername,
			String smtpPassword,
			boolean starttlsEnabled,
			boolean sslEnabled) {
	}

	public record MySettingsView(String recipientEmail, SenderSettingsView sender) {
	}

	public record SenderSettingsView(
			boolean available,
			boolean configured,
			String senderEmail,
			String senderName,
			String smtpHost,
			Integer smtpPort,
			String smtpUsername,
			boolean smtpPasswordConfigured,
			boolean starttlsEnabled,
			boolean sslEnabled) {
	}

	public record ResolvedSenderConfig(
			String senderEmail,
			String senderName,
			String smtpHost,
			Integer smtpPort,
			String smtpUsername,
			String smtpPassword,
			boolean starttlsEnabled,
			boolean sslEnabled) {
	}
}
