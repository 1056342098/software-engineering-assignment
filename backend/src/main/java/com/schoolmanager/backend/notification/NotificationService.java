package com.schoolmanager.backend.notification;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.config.AppProperties;
import com.schoolmanager.backend.notification.entity.Notification;
import com.schoolmanager.backend.notification.entity.NotificationDelivery;
import com.schoolmanager.backend.notification.entity.NotificationRecipient;
import com.schoolmanager.backend.notification.repo.NotificationDeliveryRepository;
import com.schoolmanager.backend.notification.repo.NotificationRecipientRepository;
import com.schoolmanager.backend.notification.repo.NotificationRepository;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.student.repo.ClassManagerRepository;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class NotificationService {
	private static final Set<String> SUPPORTED_CHANNELS = Set.of("IN_APP", "EMAIL");

	private final NotificationRepository notificationRepository;
	private final NotificationRecipientRepository recipientRepository;
	private final NotificationDeliveryRepository deliveryRepository;
	private final StudentRepository studentRepository;
	private final ClassManagerRepository classManagerRepository;
	private final SysUserRepository userRepository;
	private final NotificationEmailSettingsService emailSettingsService;
	private final NotificationEmailGateway notificationEmailGateway;
	private final ObjectMapper objectMapper;
	private final OperationLogService opLogService;
	private final Path notificationDir;

	public NotificationService(
			NotificationRepository notificationRepository,
			NotificationRecipientRepository recipientRepository,
			NotificationDeliveryRepository deliveryRepository,
			StudentRepository studentRepository,
			ClassManagerRepository classManagerRepository,
			SysUserRepository userRepository,
			NotificationEmailSettingsService emailSettingsService,
			NotificationEmailGateway notificationEmailGateway,
			ObjectMapper objectMapper,
			OperationLogService opLogService,
			AppProperties props) {
		this.notificationRepository = notificationRepository;
		this.recipientRepository = recipientRepository;
		this.deliveryRepository = deliveryRepository;
		this.studentRepository = studentRepository;
		this.classManagerRepository = classManagerRepository;
		this.userRepository = userRepository;
		this.emailSettingsService = emailSettingsService;
		this.notificationEmailGateway = notificationEmailGateway;
		this.objectMapper = objectMapper;
		this.opLogService = opLogService;
		this.notificationDir = Path.of(props.getStorage().getNotificationDir());
	}

	@Transactional
	public NotificationDetail create(long operatorId, Set<String> roleCodes, CreateCommand command) {
		List<String> channels = normalizeChannels(command.channels());
		if (channels.isEmpty()) {
			throw new ApiException(400, "请至少选择一个发送渠道");
		}
		SysUser creator = userRepository.findById(operatorId).orElseThrow(() -> new ApiException(404, "未找到用户"));
		List<Student> recipients = resolveTargets(operatorId, roleCodes, command.target());
		if (recipients.isEmpty()) {
			throw new ApiException(400, "未匹配到任何学生");
		}
		NotificationEmailSettingsService.ResolvedSenderConfig senderConfig = channels.contains("EMAIL")
				? emailSettingsService.requireResolvedSender(operatorId)
				: null;

		Notification notification = new Notification();
		notification.setTitle(requireText(command.title(), "标题不能为空"));
		notification.setContent(requireText(command.content(), "正文不能为空"));
		notification.setExpireAt(command.expireAt());
		notification.setTagsJson(writeJson(normalizeList(command.tags())));
		notification.setChannelsJson(writeJson(channels));
		notification.setTargetJson(writeJson(command.target() == null ? Map.of() : command.target()));
		notification.setCreatedBy(creator);
		notification = notificationRepository.save(notification);
		storeAttachment(notification, command.attachment());
		notification = notificationRepository.save(notification);

		Instant now = Instant.now();
		for (Student student : recipients) {
			NotificationRecipient recipient = new NotificationRecipient();
			recipient.setNotification(notification);
			recipient.setStudent(student);
			recipient.setReadStatus("UNREAD");
			recipient.setDeliveryStatus("SENT");
			recipient = recipientRepository.save(recipient);

			List<NotificationDelivery> deliveries = new ArrayList<>();
			for (String channel : channels) {
				DeliveryResult result = dispatch(channel, notification, creator, student, senderConfig, now);
				NotificationDelivery delivery = new NotificationDelivery();
				delivery.setRecipient(recipient);
				delivery.setChannel(channel);
				delivery.setStatus(result.status());
				delivery.setProviderMessage(result.providerMessage());
				delivery.setSentAt(result.sentAt());
				deliveries.add(delivery);
			}
			deliveryRepository.saveAll(deliveries);
			recipient.setDeliveryStatus(summarizeDeliveryStatus(deliveries));
			recipientRepository.save(recipient);
		}

		opLogService.log(operatorId, "NOTIFICATION_CREATE", "notification", notification.getId(), Map.of(
				"title", notification.getTitle(),
				"channels", channels,
				"targetCount", recipients.size()));
		return getDetailForSender(operatorId, roleCodes, notification.getId());
	}

	@Transactional(readOnly = true)
	public AttachmentDownloadView getAttachmentForDownload(long operatorId, Set<String> roleCodes,
			long notificationId) {
		Notification notification = notificationRepository.findByIdWithCreator(notificationId)
				.orElseThrow(() -> new ApiException(404, "未找到通知"));
		boolean senderAllowed = roleCodes.contains("LEADER")
				|| (notification.getCreatedBy() != null && notification.getCreatedBy().getId().equals(operatorId));
		boolean studentAllowed = recipientRepository.findByNotificationIdAndStudentId(notificationId, operatorId)
				.isPresent();
		if (!senderAllowed && !studentAllowed) {
			throw new ApiException(403, "无权限下载该附件");
		}
		if (normalize(notification.getAttachmentFilePath()) == null
				|| normalize(notification.getAttachmentName()) == null) {
			throw new ApiException(404, "该通知没有附件");
		}
		return new AttachmentDownloadView(
				notification.getAttachmentName(),
				notification.getAttachmentMimeType(),
				new FileSystemResource(notification.getAttachmentFilePath()));
	}

	@Transactional(readOnly = true)
	public List<NotificationSummary> listSent(long operatorId, Set<String> roleCodes) {
		List<Notification> notifications = roleCodes.contains("LEADER")
				? notificationRepository.findAllWithCreatorOrderByIdDesc()
				: notificationRepository.findByCreatedByIdWithCreatorOrderByIdDesc(operatorId);
		return notifications.stream().map(this::toSummary).toList();
	}

	@Transactional(readOnly = true)
	public NotificationDetail getDetailForSender(long operatorId, Set<String> roleCodes, long notificationId) {
		Notification notification = notificationRepository.findByIdWithCreator(notificationId)
				.orElseThrow(() -> new ApiException(404, "未找到通知"));
		if (!roleCodes.contains("LEADER") && !notification.getCreatedBy().getId().equals(operatorId)) {
			throw new ApiException(403, "无权限查看该通知");
		}
		List<NotificationRecipient> recipients = recipientRepository.findByNotificationIdWithStudent(notificationId);
		Map<Long, List<NotificationDelivery>> deliveriesByRecipient = groupDeliveries(recipients);
		List<RecipientView> recipientViews = recipients.stream()
				.map(recipient -> toRecipientView(recipient,
						deliveriesByRecipient.getOrDefault(recipient.getId(), List.of())))
				.toList();
		Stats stats = buildStats(recipientViews, readChannels(notification));
		return new NotificationDetail(
				notification.getId(),
				notification.getTitle(),
				notification.getContent(),
				readTags(notification),
				readChannels(notification),
				notification.getAttachmentName(),
				buildAttachmentUrl(notification),
				notification.getExpireAt(),
				new CreatorView(notification.getCreatedBy().getId(), notification.getCreatedBy().getRealName()),
				notification.getCreatedAt(),
				stats,
				recipientViews);
	}

	@Transactional(readOnly = true)
	public List<InboxItem> listInbox(long studentId) {
		List<NotificationRecipient> recipients = recipientRepository.findInboxByStudentId(studentId);
		Map<Long, List<NotificationDelivery>> deliveriesByRecipient = groupDeliveries(recipients);
		return recipients.stream()
				.map(recipient -> {
					Notification notification = recipient.getNotification();
					return new InboxItem(
							notification.getId(),
							notification.getTitle(),
							notification.getContent(),
							readTags(notification),
							readChannels(notification),
							notification.getAttachmentName(),
							buildAttachmentUrl(notification),
							notification.getExpireAt(),
							recipient.getDeliveryStatus(),
							recipient.getReadStatus(),
							recipient.getReadAt(),
							new CreatorView(notification.getCreatedBy().getId(),
									notification.getCreatedBy().getRealName()),
							notification.getCreatedAt(),
							deliveriesByRecipient.getOrDefault(recipient.getId(), List.of()).stream()
									.map(this::toChannelDeliveryView)
									.toList());
				})
				.toList();
	}

	@Transactional
	public void markRead(long studentId, long notificationId) {
		NotificationRecipient recipient = recipientRepository
				.findByNotificationIdAndStudentId(notificationId, studentId)
				.orElseThrow(() -> new ApiException(404, "未找到该通知"));
		if (!"READ".equals(recipient.getReadStatus())) {
			recipient.setReadStatus("READ");
			recipient.setReadAt(Instant.now());
			recipientRepository.save(recipient);
		}
	}

	private NotificationSummary toSummary(Notification notification) {
		List<NotificationRecipient> recipients = recipientRepository
				.findByNotificationIdWithStudent(notification.getId());
		Map<Long, List<NotificationDelivery>> deliveriesByRecipient = groupDeliveries(recipients);
		List<RecipientView> recipientViews = recipients.stream()
				.map(recipient -> toRecipientView(recipient,
						deliveriesByRecipient.getOrDefault(recipient.getId(), List.of())))
				.toList();
		return new NotificationSummary(
				notification.getId(),
				notification.getTitle(),
				notification.getContent(),
				readTags(notification),
				readChannels(notification),
				notification.getAttachmentName(),
				buildAttachmentUrl(notification),
				notification.getExpireAt(),
				new CreatorView(notification.getCreatedBy().getId(), notification.getCreatedBy().getRealName()),
				notification.getCreatedAt(),
				buildStats(recipientViews, readChannels(notification)));
	}

	private RecipientView toRecipientView(NotificationRecipient recipient, List<NotificationDelivery> deliveries) {
		Student student = recipient.getStudent();
		return new RecipientView(
				recipient.getId(),
				student.getId(),
				student.getUser() == null ? null : student.getUser().getRealName(),
				student.getStudentNo(),
				student.getClassName(),
				recipient.getDeliveryStatus(),
				recipient.getReadStatus(),
				recipient.getReadAt(),
				deliveries.stream().map(this::toChannelDeliveryView).toList());
	}

	private ChannelDeliveryView toChannelDeliveryView(NotificationDelivery delivery) {
		return new ChannelDeliveryView(
				delivery.getChannel(),
				delivery.getStatus(),
				delivery.getProviderMessage(),
				delivery.getSentAt());
	}

	private Map<Long, List<NotificationDelivery>> groupDeliveries(Collection<NotificationRecipient> recipients) {
		if (recipients.isEmpty()) {
			return Map.of();
		}
		List<Long> recipientIds = recipients.stream().map(NotificationRecipient::getId).toList();
		return deliveryRepository.findByRecipientIds(recipientIds).stream()
				.collect(Collectors.groupingBy(delivery -> delivery.getRecipient().getId(), LinkedHashMap::new,
						Collectors.toList()));
	}

	private Stats buildStats(List<RecipientView> recipients, List<String> channels) {
		int total = recipients.size();
		int read = (int) recipients.stream().filter(r -> "READ".equals(r.readStatus())).count();
		int unread = total - read;
		int failed = (int) recipients.stream().filter(r -> "FAILED".equals(r.deliveryStatus())).count();
		int partialFailed = (int) recipients.stream().filter(r -> "PARTIAL_FAILED".equals(r.deliveryStatus())).count();
		Map<String, ChannelStats> channelStats = new LinkedHashMap<>();
		for (String channel : channels) {
			int sent = 0;
			int channelFailed = 0;
			for (RecipientView recipient : recipients) {
				for (ChannelDeliveryView delivery : recipient.deliveries()) {
					if (!channel.equals(delivery.channel())) {
						continue;
					}
					if ("FAILED".equals(delivery.status())) {
						channelFailed++;
					} else {
						sent++;
					}
				}
			}
			channelStats.put(channel, new ChannelStats(sent, channelFailed));
		}
		return new Stats(total, read, unread, failed, partialFailed, channelStats);
	}

	private List<Student> resolveTargets(long operatorId, Set<String> roleCodes, TargetFilter target) {
		List<Student> accessibleStudents;
		if (roleCodes.contains("LEADER")) {
			accessibleStudents = studentRepository.findAll().stream()
					.sorted((a, b) -> a.getId().compareTo(b.getId()))
					.toList();
		} else {
			List<String> managedClasses = classManagerRepository.findClassNamesByUserId(operatorId);
			if (managedClasses.isEmpty()) {
				accessibleStudents = List.of();
			} else {
				accessibleStudents = studentRepository.findByClassNameInOrderByIdAsc(managedClasses);
			}
		}
		if (target == null) {
			return accessibleStudents;
		}
		Set<Long> studentIds = asLongSet(target.studentIds());
		Set<Integer> grades = asIntSet(target.grades());
		Set<String> classNames = asStringSet(target.classNames());
		Set<String> majors = asStringSet(target.majors());
		boolean hasStudentIds = !studentIds.isEmpty();
		boolean hasGrades = !grades.isEmpty();
		boolean hasClasses = !classNames.isEmpty();
		boolean hasMajors = !majors.isEmpty();
		return accessibleStudents.stream()
				.filter(student -> !hasStudentIds || studentIds.contains(student.getId()))
				.filter(student -> !hasGrades || (student.getGrade() != null && grades.contains(student.getGrade())))
				.filter(student -> !hasClasses || classNames.contains(student.getClassName()))
				.filter(student -> !hasMajors || majors.contains(student.getMajor()))
				.toList();
	}

	private DeliveryResult dispatch(
			String channel,
			Notification notification,
			SysUser creator,
			Student student,
			NotificationEmailSettingsService.ResolvedSenderConfig senderConfig,
			Instant now) {
		SysUser user = student.getUser();
		return switch (channel) {
			case "IN_APP" -> new DeliveryResult("SENT", "站内消息已入库", now);
			case "EMAIL" -> {
				if (user == null || normalize(user.getEmail()) == null) {
					yield new DeliveryResult("FAILED", "学生未配置邮箱", null);
				}
				try {
					String providerMessage = notificationEmailGateway.send(new NotificationEmailGateway.SendCommand(
							senderConfig.senderEmail(),
							senderConfig.senderName(),
							senderConfig.smtpHost(),
							senderConfig.smtpPort(),
							senderConfig.smtpUsername(),
							senderConfig.smtpPassword(),
							senderConfig.starttlsEnabled(),
							senderConfig.sslEnabled(),
							user.getEmail(),
							notification.getTitle(),
							buildEmailBody(notification, creator, student),
							buildEmailAttachments(notification)));
					yield new DeliveryResult("SENT", providerMessage, now);
				} catch (Exception e) {
					yield new DeliveryResult("FAILED", safeProviderMessage(e), null);
				}
			}
			default -> throw new ApiException(400, "不支持的通知渠道: " + channel);
		};
	}

	private static String buildEmailBody(Notification notification, SysUser creator, Student student) {
		StringBuilder sb = new StringBuilder();
		sb.append("您好");
		if (student.getUser() != null && normalize(student.getUser().getRealName()) != null) {
			sb.append("，").append(student.getUser().getRealName());
		}
		sb.append("：\n\n");
		sb.append(notification.getContent()).append("\n\n");
		sb.append("发布人：").append(creator.getRealName()).append("\n");
		if (notification.getExpireAt() != null) {
			sb.append("截止时间：").append(notification.getExpireAt()).append("\n");
		}
		if (normalize(notification.getAttachmentName()) != null) {
			sb.append("附件：").append(notification.getAttachmentName()).append("（已随邮件附带）\n");
		}
		sb.append("\n此邮件由校园管理系统自动发送。");
		return sb.toString();
	}

	private List<NotificationEmailGateway.Attachment> buildEmailAttachments(Notification notification) {
		if (normalize(notification.getAttachmentFilePath()) == null
				|| normalize(notification.getAttachmentName()) == null) {
			return List.of();
		}
		return List.of(new NotificationEmailGateway.Attachment(
				notification.getAttachmentName(),
				notification.getAttachmentFilePath(),
				notification.getAttachmentMimeType() == null ? "application/octet-stream"
						: notification.getAttachmentMimeType()));
	}

	private String buildAttachmentUrl(Notification notification) {
		if (normalize(notification.getAttachmentFilePath()) == null
				|| normalize(notification.getAttachmentName()) == null) {
			return null;
		}
		return "/api/notifications/" + notification.getId() + "/attachment/download";
	}

	private void storeAttachment(Notification notification, MultipartFile attachment) {
		if (attachment == null || attachment.isEmpty()) {
			notification.setAttachmentName(null);
			notification.setAttachmentUrl(null);
			notification.setAttachmentFilePath(null);
			notification.setAttachmentMimeType(null);
			notification.setAttachmentFileSize(null);
			return;
		}
		validateAttachment(attachment);
		Path savedPath = null;
		try {
			Files.createDirectories(notificationDir);
			String originalName = sanitizeOriginalName(attachment.getOriginalFilename(), "notification-attachment");
			String safeName = originalName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
			savedPath = notificationDir
					.resolve(notification.getId() + "_" + Instant.now().toEpochMilli() + "_" + safeName);
			Files.copy(attachment.getInputStream(), savedPath);
			notification.setAttachmentName(originalName);
			notification.setAttachmentUrl("/api/notifications/" + notification.getId() + "/attachment/download");
			notification.setAttachmentFilePath(savedPath.toAbsolutePath().toString());
			notification.setAttachmentMimeType(normalize(attachment.getContentType()));
			notification.setAttachmentFileSize(attachment.getSize());
		} catch (Exception e) {
			if (savedPath != null) {
				try {
					Files.deleteIfExists(savedPath);
				} catch (Exception ignored) {
				}
			}
			throw new ApiException(500, "通知附件保存失败");
		}
	}

	private static void validateAttachment(MultipartFile attachment) {
		String originalName = sanitizeOriginalName(attachment.getOriginalFilename(), "notification-attachment");
		String lowerName = originalName.toLowerCase(Locale.ROOT);
		if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".ppt") && !lowerName.endsWith(".pptx")
				&& !lowerName.endsWith(".doc") && !lowerName.endsWith(".docx") && !lowerName.endsWith(".txt")
				&& !lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")
				&& !lowerName.endsWith(".xls") && !lowerName.endsWith(".xlsx") && !lowerName.endsWith(".zip")) {
			throw new ApiException(400, "通知附件类型不支持");
		}
		if (attachment.getSize() > 30L * 1024 * 1024) {
			throw new ApiException(400, "通知附件不能超过 30MB");
		}
	}

	private static String sanitizeOriginalName(String originalName, String fallback) {
		String resolved = originalName == null ? fallback : originalName;
		resolved = resolved.replace("\\", "/");
		int lastSlash = resolved.lastIndexOf('/');
		if (lastSlash >= 0) {
			resolved = resolved.substring(lastSlash + 1);
		}
		if (resolved.isBlank()) {
			return fallback;
		}
		return resolved;
	}

	private static String safeProviderMessage(Exception e) {
		String message = e.getMessage();
		String normalized = normalize(message);
		if (normalized == null) {
			return "SMTP 发送失败";
		}
		return normalized.length() > 240 ? normalized.substring(0, 240) : normalized;
	}

	private static String summarizeDeliveryStatus(List<NotificationDelivery> deliveries) {
		long successCount = deliveries.stream().filter(delivery -> !"FAILED".equals(delivery.getStatus())).count();
		if (successCount == 0) {
			return "FAILED";
		}
		if (successCount < deliveries.size()) {
			return "PARTIAL_FAILED";
		}
		return "SENT";
	}

	private List<String> normalizeChannels(List<String> channels) {
		List<String> normalized = normalizeList(channels).stream()
				.map(String::toUpperCase)
				.distinct()
				.toList();
		for (String channel : normalized) {
			if (!SUPPORTED_CHANNELS.contains(channel)) {
				throw new ApiException(400, "不支持的通知渠道: " + channel);
			}
		}
		return normalized;
	}

	private static List<String> normalizeList(List<String> values) {
		if (values == null) {
			return List.of();
		}
		return values.stream()
				.map(NotificationService::normalize)
				.filter(value -> value != null)
				.distinct()
				.toList();
	}

	private static Set<Long> asLongSet(List<Long> values) {
		return values == null ? Set.of() : new LinkedHashSet<>(values);
	}

	private static Set<Integer> asIntSet(List<Integer> values) {
		return values == null ? Set.of() : new LinkedHashSet<>(values);
	}

	private static Set<String> asStringSet(List<String> values) {
		return normalizeList(values).stream().collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private List<String> readTags(Notification notification) {
		return readStringList(notification.getTagsJson());
	}

	private List<String> readChannels(Notification notification) {
		return readStringList(notification.getChannelsJson());
	}

	private List<String> readStringList(String json) {
		if (json == null || json.isBlank()) {
			return List.of();
		}
		try {
			return objectMapper.readValue(json, new TypeReference<List<String>>() {
			});
		} catch (Exception e) {
			return List.of();
		}
	}

	private String writeJson(Object value) {
		try {
			return objectMapper.writeValueAsString(value);
		} catch (Exception e) {
			throw new ApiException(400, "数据格式无效");
		}
	}

	private static String requireText(String value, String message) {
		String normalized = normalize(value);
		if (normalized == null) {
			throw new ApiException(400, message);
		}
		return normalized;
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String trimmed = value.strip();
		return trimmed.isBlank() ? null : trimmed;
	}

	private record DeliveryResult(String status, String providerMessage, Instant sentAt) {
	}

	public record CreateCommand(
			String title,
			String content,
			Instant expireAt,
			List<String> tags,
			List<String> channels,
			TargetFilter target,
			MultipartFile attachment) {
	}

	public record TargetFilter(List<Long> studentIds, List<Integer> grades, List<String> classNames,
			List<String> majors) {
	}

	public record CreatorView(Long id, String name) {
	}

	public record ChannelStats(int sent, int failed) {
	}

	public record Stats(int total, int read, int unread, int failed, int partialFailed,
			Map<String, ChannelStats> channels) {
	}

	public record ChannelDeliveryView(String channel, String status, String providerMessage, Instant sentAt) {
	}

	public record RecipientView(Long recipientId, Long studentId, String studentName, String studentNo,
			String className,
			String deliveryStatus, String readStatus, Instant readAt, List<ChannelDeliveryView> deliveries) {
	}

	public record NotificationSummary(Long id, String title, String content, List<String> tags, List<String> channels,
			String attachmentName, String attachmentUrl, Instant expireAt, CreatorView creator, Instant createdAt,
			Stats stats) {
	}

	public record NotificationDetail(Long id, String title, String content, List<String> tags, List<String> channels,
			String attachmentName, String attachmentUrl, Instant expireAt, CreatorView creator, Instant createdAt,
			Stats stats, List<RecipientView> recipients) {
	}

	public record InboxItem(Long id, String title, String content, List<String> tags, List<String> channels,
			String attachmentName, String attachmentUrl, Instant expireAt, String deliveryStatus, String readStatus,
			Instant readAt, CreatorView creator, Instant createdAt, List<ChannelDeliveryView> deliveries) {
	}

	public record AttachmentDownloadView(String fileName, String mimeType, Resource resource) {
	}
}
