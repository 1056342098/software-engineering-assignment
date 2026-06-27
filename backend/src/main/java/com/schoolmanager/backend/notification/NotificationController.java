package com.schoolmanager.backend.notification;

import com.schoolmanager.backend.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
	private final NotificationService notificationService;
	private final CurrentUser currentUser;
	private final ObjectMapper objectMapper;

	public NotificationController(NotificationService notificationService, CurrentUser currentUser,
			ObjectMapper objectMapper) {
		this.notificationService = notificationService;
		this.currentUser = currentUser;
		this.objectMapper = objectMapper;
	}

	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('TEACHER','LEADER','CADRE')")
	public ApiResponse<NotificationService.NotificationDetail> create(
			@RequestParam("title") @NotBlank String title,
			@RequestParam("content") @NotBlank String content,
			@RequestParam(value = "expireAt", required = false) String expireAt,
			@RequestParam(value = "tags", required = false) List<String> tags,
			@RequestParam("channels") @NotEmpty List<String> channels,
			@RequestParam(value = "targetJson", required = false) String targetJson,
			@RequestParam(value = "attachment", required = false) MultipartFile attachment) {
		TargetReq target = parseTarget(targetJson);
		NotificationService.NotificationDetail detail = notificationService.create(
				currentUser.id(),
				currentUser.roleCodes(),
				new NotificationService.CreateCommand(
						title,
						content,
						parseInstant(expireAt),
						tags,
						channels,
						target == null ? null
								: new NotificationService.TargetFilter(
										target.studentIds(),
										target.grades(),
										target.classNames(),
										target.majors()),
						attachment));
		return ApiResponse.ok(detail);
	}

	@GetMapping("/sent")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER','CADRE')")
	public ApiResponse<List<NotificationService.NotificationSummary>> listSent() {
		return ApiResponse.ok(notificationService.listSent(currentUser.id(), currentUser.roleCodes()));
	}

	@GetMapping("/{notificationId}")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER','CADRE')")
	public ApiResponse<NotificationService.NotificationDetail> detail(@PathVariable long notificationId) {
		return ApiResponse
				.ok(notificationService.getDetailForSender(currentUser.id(), currentUser.roleCodes(), notificationId));
	}

	@GetMapping("/my")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<List<NotificationService.InboxItem>> myInbox() {
		return ApiResponse.ok(notificationService.listInbox(currentUser.id()));
	}

	@PostMapping("/{notificationId}/read")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<Void> markRead(@PathVariable long notificationId) {
		notificationService.markRead(currentUser.id(), notificationId);
		return ApiResponse.ok(null);
	}

	@GetMapping("/{notificationId}/attachment/download")
	public ResponseEntity<Resource> downloadAttachment(@PathVariable long notificationId) {
		NotificationService.AttachmentDownloadView attachment = notificationService.getAttachmentForDownload(
				currentUser.id(),
				currentUser.roleCodes(),
				notificationId);
		return ResponseEntity.ok()
				.contentType(parseMediaType(attachment.mimeType()))
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + attachment.fileName() + "\"")
				.body(attachment.resource());
	}

	public record TargetReq(List<Long> studentIds, List<Integer> grades, List<String> classNames, List<String> majors) {
	}

	private TargetReq parseTarget(String targetJson) {
		if (targetJson == null || targetJson.isBlank()) {
			return null;
		}
		try {
			return objectMapper.readValue(targetJson, TargetReq.class);
		} catch (Exception e) {
			throw new ApiException(400, "通知接收范围格式无效");
		}
	}

	private static Instant parseInstant(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		try {
			return Instant.parse(value);
		} catch (Exception e) {
			throw new ApiException(400, "截止时间格式无效");
		}
	}

	private static MediaType parseMediaType(String mimeType) {
		if (mimeType == null || mimeType.isBlank()) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
		try {
			return MediaType.parseMediaType(mimeType);
		} catch (Exception e) {
			return MediaType.APPLICATION_OCTET_STREAM;
		}
	}
}
