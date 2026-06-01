package com.schoolmanager.backend.approval;

import com.schoolmanager.backend.approval.entity.ApprovalAssignee;
import com.schoolmanager.backend.approval.entity.ApprovalAttachment;
import com.schoolmanager.backend.approval.entity.Approval;
import com.schoolmanager.backend.approval.repo.ApprovalAssigneeRepository;
import com.schoolmanager.backend.approval.repo.ApprovalAttachmentRepository;
import com.schoolmanager.backend.approval.repo.ApprovalRepository;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {
	private final ApprovalService approvalService;
	private final ApprovalRepository approvalRepository;
	private final ApprovalAssigneeRepository assigneeRepository;
	private final ApprovalAttachmentRepository attachmentRepository;
	private final CurrentUser currentUser;

	public ApprovalController(ApprovalService approvalService, ApprovalRepository approvalRepository,
			ApprovalAssigneeRepository assigneeRepository,
			ApprovalAttachmentRepository attachmentRepository,
			CurrentUser currentUser) {
		this.approvalService = approvalService;
		this.approvalRepository = approvalRepository;
		this.assigneeRepository = assigneeRepository;
		this.attachmentRepository = attachmentRepository;
		this.currentUser = currentUser;
	}

	@GetMapping("/me")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<List<ApprovalDto>> myList() {
		return ApiResponse.ok(approvalService.listMy(currentUser.id()).stream().map(ApprovalDto::from).toList());
	}

	@PostMapping("/apply")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<ApprovalDto> apply(
			@RequestParam("type") @NotBlank String type,
			@RequestParam("subject") @NotBlank String subject,
			@RequestParam(value = "content", required = false) String content,
			@RequestParam("approverIds") @NotBlank String approverIds,
			@RequestParam(value = "files", required = false) List<MultipartFile> files) {
		List<Long> ids = Arrays.stream(approverIds.split(","))
				.map(String::trim)
				.filter(s -> !s.isBlank())
				.map(Long::valueOf)
				.toList();
		Approval approval = approvalService.createV2(currentUser.id(), type, subject, content, ids, files);
		return ApiResponse.ok(ApprovalDto.from(approval));
	}

	@GetMapping("/pending")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<List<ApprovalDto>> pending() {
		return ApiResponse
				.ok(approvalService.listPendingForApprover(currentUser.id()).stream().map(ApprovalDto::from).toList());
	}

	@GetMapping("/history")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<List<ApprovalDto>> history() {
		return ApiResponse.ok(
				approvalService.listHistoryForApprover(currentUser.id()).stream().map(ApprovalDto::from).toList()
		);
	}

	@GetMapping("/progress/me")
	@PreAuthorize("hasRole('STUDENT')")
	public ApiResponse<ProgressResp> myProgress() {
		long uid = currentUser.id();
		var items = approvalService.getProgressViews(uid).stream()
				.map(p -> new ProgressItemDto(
						p.approvalType(),
						p.stageIndex(),
						p.stageCode(),
						p.stageName(),
						p.stages().stream().map(s -> new StageDto(s.code(), s.name())).toList(),
						p.lastResult(),
						p.lastAssessedAt(),
						p.nextDueAt(),
						p.lastApprovalId(),
						p.pendingApprovalId()))
				.toList();
		return ApiResponse.ok(new ProgressResp(Instant.now(), items));
	}

	@GetMapping("/{approvalId}")
	public ApiResponse<ApprovalDetailDto> detail(@PathVariable long approvalId) {
		ensureReadable(approvalId);
		Approval approval = approvalRepository.findByIdWithApplicant(approvalId)
				.orElseThrow(() -> new ApiException(404, "APPROVAL_NOT_FOUND"));
		List<ApprovalAssignee> assignees = approvalService.listAssignees(approvalId);
		List<ApprovalAttachment> atts = approvalService.listAttachments(approvalId);
		return ApiResponse.ok(ApprovalDetailDto.from(approval, assignees, atts));
	}

	@GetMapping("/{approvalId}/attachments/{attachmentId}/download")
	public ResponseEntity<Resource> downloadAttachment(@PathVariable long approvalId, @PathVariable long attachmentId) {
		ensureReadable(approvalId);
		ApprovalAttachment att = attachmentRepository.findById(attachmentId)
				.orElseThrow(() -> new ApiException(404, "ATTACHMENT_NOT_FOUND"));
		if (att.getApproval() == null || att.getApproval().getId() == null || att.getApproval().getId() != approvalId) {
			throw new ApiException(404, "ATTACHMENT_NOT_FOUND");
		}
		Resource file = approvalService.getAttachmentFile(approvalId, attachmentId);
		return ResponseEntity.ok()
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + att.getFileName() + "\"")
				.body(file);
	}

	@GetMapping("/assigned")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<List<ApprovalDto>> assigned() {
		return ApiResponse.ok(approvalService.listAssigned(currentUser.id()).stream().map(ApprovalDto::from).toList());
	}

	@PostMapping("/{approvalId}/approve")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<Void> approve(@PathVariable long approvalId, @RequestBody Map<String, Object> body) {
		approvalService.approve(currentUser.id(), approvalId, body == null ? null : str(body.get("comment")));
		return ApiResponse.ok(null);
	}

	@PostMapping("/{approvalId}/reject")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<Void> reject(@PathVariable long approvalId, @RequestBody Map<String, Object> body) {
		approvalService.reject(currentUser.id(), approvalId, body == null ? null : str(body.get("comment")));
		return ApiResponse.ok(null);
	}

	@PostMapping("/{approvalId}/revoke")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<Void> revoke(@PathVariable long approvalId, @RequestBody Map<String, Object> body) {
		approvalService.revoke(currentUser.id(), approvalId, body == null ? null : str(body.get("comment")));
		return ApiResponse.ok(null);
	}

	@GetMapping("/{approvalId}/steps")
	public ApiResponse<List<Map<String, Object>>> steps(@PathVariable long approvalId) {
		ensureReadable(approvalId);
		var steps = approvalService.getSteps(approvalId).stream().map(s -> Map.<String, Object>of(
				"stepNo", s.getStepNo(),
				"name", s.getName(),
				"status", s.getStatus(),
				"dueAt", s.getDueAt(),
				"actedBy", s.getActedBy(),
				"actedAt", s.getActedAt(),
				"comment", s.getComment())).toList();
		return ApiResponse.ok(steps);
	}

	@GetMapping("/{approvalId}/logs")
	public ApiResponse<List<Map<String, Object>>> logs(@PathVariable long approvalId) {
		ensureReadable(approvalId);
		var logs = approvalService.getLogs(approvalId).stream().map(l -> Map.<String, Object>of(
				"action", l.getAction(),
				"operatorId", l.getOperatorId(),
				"comment", l.getComment(),
				"opTime", l.getOpTime())).toList();
		return ApiResponse.ok(logs);
	}

	private void ensureReadable(long approvalId) {
		Approval approval = approvalRepository.findByIdWithApplicant(approvalId)
				.orElseThrow(() -> new ApiException(404, "APPROVAL_NOT_FOUND"));
		long uid = currentUser.id();
		if (approval.getApplicant() != null && approval.getApplicant().getId() != null
				&& approval.getApplicant().getId() == uid) {
			return;
		}
		boolean isAssignee = assigneeRepository.findOne(approvalId, uid).isPresent();
		if (isAssignee)
			return;
		throw new ApiException(403, "FORBIDDEN");
	}

	private static String str(Object v) {
		return v == null ? null : String.valueOf(v);
	}

	public record ApprovalDto(Long id, Long applicantId, String applicantName, Long finalApproverId, String type,
			String subject, String status) {
		static ApprovalDto from(Approval a) {
			Long applicantId = a.getApplicant() == null ? null : a.getApplicant().getId();
			String applicantName = a.getApplicant() == null ? null : a.getApplicant().getRealName();
			Long approverId = a.getApprover() == null ? null : a.getApprover().getId();
			return new ApprovalDto(a.getId(), applicantId, applicantName, approverId, a.getType(), a.getSubject(),
					a.getStatus());
		}
	}

	public record ApprovalDetailDto(
			Long id,
			Long applicantId,
			String applicantName,
			String type,
			String subject,
			String content,
			String status,
			List<AssigneeDto> assignees,
			List<AttachmentDto> attachments) {
		static ApprovalDetailDto from(Approval a, List<ApprovalAssignee> assignees, List<ApprovalAttachment> atts) {
			List<AssigneeDto> as = assignees.stream().map(x -> new AssigneeDto(
					x.getApprover() == null ? null : x.getApprover().getId(),
					x.getApprover() == null ? null : x.getApprover().getRealName(),
					x.getStatus())).toList();
			List<AttachmentDto> ds = atts.stream().map(x -> new AttachmentDto(
					x.getId(),
					x.getFileName(),
					x.getFileSize())).toList();
			return new ApprovalDetailDto(
					a.getId(),
					a.getApplicant() == null ? null : a.getApplicant().getId(),
					a.getApplicant() == null ? null : a.getApplicant().getRealName(),
					a.getType(),
					a.getSubject(),
					a.getContent(),
					a.getStatus(),
					as,
					ds);
		}
	}

	public record AssigneeDto(Long id, String name, String status) {
	}

	public record AttachmentDto(Long id, String fileName, Long size) {
	}

	public record ProgressResp(Instant serverNow, List<ProgressItemDto> items) {
	}

	public record ProgressItemDto(
			String approvalType,
			int stageIndex,
			String stageCode,
			String stageName,
			List<StageDto> stages,
			String lastResult,
			Instant lastAssessedAt,
			Instant nextDueAt,
			Long lastApprovalId,
			Long pendingApprovalId) {
	}

	public record StageDto(String code, String name) {
	}
}
