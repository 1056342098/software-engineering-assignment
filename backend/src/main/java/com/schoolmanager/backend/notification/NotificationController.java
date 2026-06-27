package com.schoolmanager.backend.notification;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
	private final NotificationService notificationService;
	private final CurrentUser currentUser;

	public NotificationController(NotificationService notificationService, CurrentUser currentUser) {
		this.notificationService = notificationService;
		this.currentUser = currentUser;
	}

	@PostMapping
	@PreAuthorize("hasAnyRole('TEACHER','LEADER','CADRE')")
	public ApiResponse<NotificationService.NotificationDetail> create(@Valid @RequestBody CreateReq req) {
		NotificationService.NotificationDetail detail = notificationService.create(
				currentUser.id(),
				currentUser.roleCodes(),
				new NotificationService.CreateCommand(
						req.title(),
						req.content(),
						req.attachmentName(),
						req.attachmentUrl(),
						req.expireAt(),
						req.tags(),
						req.channels(),
						req.target() == null ? null : new NotificationService.TargetFilter(
								req.target().studentIds(),
								req.target().grades(),
								req.target().classNames(),
								req.target().majors())));
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
		return ApiResponse.ok(notificationService.getDetailForSender(currentUser.id(), currentUser.roleCodes(), notificationId));
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

	public record CreateReq(
			@NotBlank String title,
			@NotBlank String content,
			String attachmentName,
			String attachmentUrl,
			Instant expireAt,
			List<String> tags,
			@NotEmpty List<String> channels,
			TargetReq target) {
	}

	public record TargetReq(List<Long> studentIds, List<Integer> grades, List<String> classNames, List<String> majors) {
	}
}
