package com.schoolmanager.backend.approval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.approval.entity.ApprovalAssignee;
import com.schoolmanager.backend.approval.entity.ApprovalAttachment;
import com.schoolmanager.backend.approval.entity.Approval;
import com.schoolmanager.backend.approval.entity.ApprovalLog;
import com.schoolmanager.backend.approval.entity.ApprovalStep;
import com.schoolmanager.backend.approval.repo.ApprovalAssigneeRepository;
import com.schoolmanager.backend.approval.repo.ApprovalAttachmentRepository;
import com.schoolmanager.backend.approval.repo.ApprovalLogRepository;
import com.schoolmanager.backend.approval.repo.ApprovalRepository;
import com.schoolmanager.backend.approval.repo.ApprovalStepRepository;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.config.AppProperties;
import com.schoolmanager.backend.oplog.OperationLogService;
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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ApprovalService {
	public static final String TYPE_PARTY = "PARTY_APPLY";
	public static final String TYPE_LEAGUE = "LEAGUE_APPLY";
	public static final String TYPE_OTHER = "OTHER";

	public static final String STATUS_PENDING = "PENDING";
	public static final String STATUS_APPROVED = "APPROVED";
	public static final String STATUS_REJECTED = "REJECTED";
	public static final String STATUS_REVOKED = "REVOKED";

	private final ApprovalRepository approvalRepository;
	private final ApprovalAssigneeRepository assigneeRepository;
	private final ApprovalAttachmentRepository attachmentRepository;
	private final ApprovalStepRepository stepRepository;
	private final ApprovalLogRepository logRepository;
	private final SysUserRepository userRepository;
	private final ObjectMapper objectMapper;
	private final OperationLogService opLogService;
	private final ApprovalProcessProgressService progressService;
	private final Path approvalDir;

	public ApprovalService(
			ApprovalRepository approvalRepository,
			ApprovalAssigneeRepository assigneeRepository,
			ApprovalAttachmentRepository attachmentRepository,
			ApprovalStepRepository stepRepository,
			ApprovalLogRepository logRepository,
			SysUserRepository userRepository,
			ObjectMapper objectMapper,
			OperationLogService opLogService,
			ApprovalProcessProgressService progressService,
			AppProperties props) {
		this.approvalRepository = approvalRepository;
		this.assigneeRepository = assigneeRepository;
		this.attachmentRepository = attachmentRepository;
		this.stepRepository = stepRepository;
		this.logRepository = logRepository;
		this.userRepository = userRepository;
		this.objectMapper = objectMapper;
		this.opLogService = opLogService;
		this.progressService = progressService;
		this.approvalDir = Path.of(props.getStorage().getApprovalDir());
	}

	public List<Approval> listMy(long applicantId) {
		return approvalRepository.findByApplicant_IdOrderByIdDesc(applicantId);
	}

	public List<Approval> listPendingForApprover(long approverId) {
		List<Long> ids = assigneeRepository.findApprovalIdsByApproverAndStatus(approverId, STATUS_PENDING);
		if (ids.isEmpty()) {
			return List.of();
		}
		return approvalRepository.findByIdInWithApplicant(ids).stream()
				.filter(a -> STATUS_PENDING.equals(a.getStatus()))
				.toList();
	}

	public List<Approval> listHistoryForApprover(long approverId) {
		List<Long> ids = assigneeRepository.findApprovalIdsByApprover(approverId);
		if (ids.isEmpty()) {
			return List.of();
		}
		return approvalRepository.findByIdInWithApplicant(ids).stream()
				.filter(a -> !STATUS_PENDING.equals(a.getStatus()))
				.toList();
	}

	public List<Approval> listAssigned(long approverId) {
		List<Long> ids = assigneeRepository.findApprovalIdsByApprover(approverId);
		if (ids.isEmpty()) {
			return List.of();
		}
		return approvalRepository.findByIdInWithApplicant(ids);
	}

	public List<ApprovalAssignee> listAssignees(long approvalId) {
		return assigneeRepository.findByApprovalId(approvalId);
	}

	public List<ApprovalAttachment> listAttachments(long approvalId) {
		return attachmentRepository.findByApprovalId(approvalId);
	}

	public Resource getAttachmentFile(long approvalId, long attachmentId) {
		ApprovalAttachment att = attachmentRepository.findById(attachmentId)
				.orElseThrow(() -> new ApiException(404, "未找到该附件"));
		if (att.getApproval() == null || att.getApproval().getId() == null || att.getApproval().getId() != approvalId) {
			throw new ApiException(404, "未找到该附件");
		}
		return new FileSystemResource(att.getFilePath());
	}

	public List<ApprovalStep> getSteps(long approvalId) {
		return stepRepository.findByApproval_IdOrderByStepNoAsc(approvalId);
	}

	public List<ApprovalLog> getLogs(long approvalId) {
		return logRepository.findByApproval_IdOrderByOpTimeAsc(approvalId);
	}

	public List<ApprovalProcessProgressService.ProgressView> getProgressViews(long userId) {
		return List.of(
				progressService.getView(userId, TYPE_PARTY),
				progressService.getView(userId, TYPE_LEAGUE));
	}

	@Transactional
	public Approval create(long applicantId, String type, long approverId, Map<String, Object> form) {
		if (!TYPE_PARTY.equals(type) && !TYPE_LEAGUE.equals(type) && !TYPE_OTHER.equals(type)) {
			throw new ApiException(400, "无效的申请类型");
		}
		SysUser applicant = userRepository.findById(applicantId)
				.orElseThrow(() -> new ApiException(404, "未找到申请人信息"));
		SysUser approver = userRepository.findById(approverId)
				.orElseThrow(() -> new ApiException(404, "未找到审批人信息"));

		Approval approval = new Approval();
		approval.setApplicant(applicant);
		approval.setApprover(null);
		approval.setType(type);
		approval.setStatus(STATUS_PENDING);
		approval.setSubject(form == null ? null : str(form.get("subject")));
		approval.setContent(form == null ? null : str(form.get("content")));
		approval.setCurrentStep(2);
		approval.setWindowExpireAt(Instant.now().plus(48, ChronoUnit.HOURS));
		try {
			approval.setFormJson(objectMapper.writeValueAsString(form));
		} catch (Exception e) {
			throw new ApiException(400, "表单数据格式无效");
		}
		approval = approvalRepository.save(approval);

		ApprovalStep s1 = new ApprovalStep();
		s1.setApproval(approval);
		s1.setStepNo(1);
		s1.setName("学生提交申请");
		s1.setStatus("DONE");
		s1.setActedBy(applicantId);
		s1.setActedAt(Instant.now());
		stepRepository.save(s1);

		ApprovalStep s2 = new ApprovalStep();
		s2.setApproval(approval);
		s2.setStepNo(2);
		s2.setName("老师审批");
		s2.setStatus("PENDING");
		stepRepository.save(s2);

		ApprovalAssignee aa = new ApprovalAssignee();
		aa.setApproval(approval);
		aa.setApprover(approver);
		aa.setStatus(STATUS_PENDING);
		assigneeRepository.save(aa);

		appendLog(approval, applicantId, "SUBMIT", null);
		opLogService.log(applicantId, "APPROVAL_SUBMIT", "approval", approval.getId(), Map.of(
				"type", type,
				"approverId", approverId));
		return approval;
	}

	@Transactional
	public Approval createV2(
			long applicantId,
			String type,
			String subject,
			String content,
			List<Long> approverIds,
			List<MultipartFile> files) {
		if (!TYPE_PARTY.equals(type) && !TYPE_LEAGUE.equals(type) && !TYPE_OTHER.equals(type)) {
			throw new ApiException(400, "无效的申请类型");
		}
		String sub = subject == null ? "" : subject.strip();
		if (sub.isBlank()) {
			throw new ApiException(400, "必须填写主题");
		}
		if (approverIds == null || approverIds.isEmpty()) {
			throw new ApiException(400, "必须选择至少一位审批人");
		}
		List<Long> uniqueApproverIds = approverIds.stream().distinct().toList();

		SysUser applicant = userRepository.findById(applicantId)
				.orElseThrow(() -> new ApiException(404, "未找到申请人信息"));

		List<SysUser> approvers = new ArrayList<>();
		for (Long id : uniqueApproverIds) {
			if (id == null) {
				continue;
			}
			SysUser u = userRepository.findById(id).orElseThrow(() -> new ApiException(404, "未找到审批人信息"));
			approvers.add(u);
		}
		if (approvers.isEmpty()) {
			throw new ApiException(400, "必须选择至少一位审批人");
		}

		Instant now = Instant.now();
		Approval approval = new Approval();
		approval.setApplicant(applicant);
		approval.setApprover(null);
		approval.setType(type);
		approval.setStatus(STATUS_PENDING);
		approval.setSubject(sub);
		approval.setContent(content == null ? null : content.strip());
		approval.setCurrentStep(2);
		approval.setWindowExpireAt(now.plus(48, ChronoUnit.HOURS));
		if (TYPE_PARTY.equals(type) || TYPE_LEAGUE.equals(type)) {
			var stage = progressService.prepareSubmission(applicantId, type, now);
			approval.setStageIndex(stage.stageIndex());
			approval.setStageCode(stage.stageCode());
		}
		try {
			Map<String, Object> formMap = new java.util.HashMap<>();
			formMap.put("subject", approval.getSubject());
			formMap.put("content", approval.getContent());
			formMap.put("approverIds", uniqueApproverIds);
			approval.setFormJson(objectMapper.writeValueAsString(formMap));
		} catch (Exception e) {
			throw new ApiException(400, "表单数据格式无效");
		}
		approval = approvalRepository.save(approval);
		if (TYPE_PARTY.equals(type) || TYPE_LEAGUE.equals(type)) {
			progressService.onSubmitted(approval);
		}

		ApprovalStep s1 = new ApprovalStep();
		s1.setApproval(approval);
		s1.setStepNo(1);
		s1.setName("学生提交申请");
		s1.setStatus("DONE");
		s1.setActedBy(applicantId);
		s1.setActedAt(Instant.now());
		stepRepository.save(s1);

		ApprovalStep s2 = new ApprovalStep();
		s2.setApproval(approval);
		s2.setStepNo(2);
		s2.setName("老师审批（任一通过即可）");
		s2.setStatus("PENDING");
		stepRepository.save(s2);

		for (SysUser approver : approvers) {
			ApprovalAssignee aa = new ApprovalAssignee();
			aa.setApproval(approval);
			aa.setApprover(approver);
			aa.setStatus(STATUS_PENDING);
			assigneeRepository.save(aa);
		}

		saveAttachments(applicant, approval, files);

		appendLog(approval, applicantId, "SUBMIT", null);
		opLogService.log(applicantId, "APPROVAL_SUBMIT", "approval", approval.getId(), Map.of(
				"type", type,
				"approverIds", uniqueApproverIds));
		return approval;
	}

	@Transactional
	public void approve(long operatorId, long approvalId, String comment) {
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new ApiException(404, "未找到该审批"));
		if (!STATUS_PENDING.equals(approval.getStatus())) {
			throw new ApiException(400, "该审批已处理");
		}
		Instant now = Instant.now();
		ApprovalAssignee aa = assigneeRepository.findOne(approvalId, operatorId)
				.orElseThrow(() -> new ApiException(403, "您不是该审批的指定审批人"));
		if (!STATUS_PENDING.equals(aa.getStatus())) {
			throw new ApiException(400, "您已处理过该审批");
		}
		ApprovalStep step = stepRepository.findByApproval_IdOrderByStepNoAsc(approvalId).stream()
				.filter(s -> s.getStepNo() == 2)
				.findFirst()
				.orElseThrow(() -> new ApiException(500, "审批步骤缺失"));
		step.setStatus("DONE");
		step.setActedBy(operatorId);
		step.setActedAt(now);
		step.setComment(comment);
		stepRepository.save(step);

		aa.setStatus(STATUS_APPROVED);
		aa.setActedAt(now);
		aa.setComment(comment);
		assigneeRepository.save(aa);

		approval.setStatus(STATUS_APPROVED);
		approval.setCurrentStep(2);
		approval.setApprover(userRepository.findById(operatorId).orElse(null));
		approvalRepository.save(approval);
		progressService.onDecided(approval, STATUS_APPROVED, now);
		appendLog(approval, operatorId, "APPROVE", comment);
		opLogService.log(operatorId, "APPROVAL_APPROVE", "approval", approvalId, comment == null ? null : Map.of("comment", comment));
	}

	@Transactional
	public void reject(long operatorId, long approvalId, String comment) {
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new ApiException(404, "未找到该审批"));
		if (!STATUS_PENDING.equals(approval.getStatus())) {
			throw new ApiException(400, "该审批已处理");
		}
		Instant now = Instant.now();
		ApprovalAssignee aa = assigneeRepository.findOne(approvalId, operatorId)
				.orElseThrow(() -> new ApiException(403, "您不是该审批的指定审批人"));
		if (!STATUS_PENDING.equals(aa.getStatus())) {
			throw new ApiException(400, "您已处理过该审批");
		}
		ApprovalStep step = stepRepository.findByApproval_IdOrderByStepNoAsc(approvalId).stream()
				.filter(s -> s.getStepNo() == 2)
				.findFirst()
				.orElseThrow(() -> new ApiException(500, "审批步骤缺失"));
		step.setStatus("REJECTED");
		step.setActedBy(operatorId);
		step.setActedAt(now);
		step.setComment(comment);
		stepRepository.save(step);

		aa.setStatus(STATUS_REJECTED);
		aa.setActedAt(now);
		aa.setComment(comment);
		assigneeRepository.save(aa);

		approval.setStatus(STATUS_REJECTED);
		approval.setApprover(userRepository.findById(operatorId).orElse(null));
		approvalRepository.save(approval);
		progressService.onDecided(approval, STATUS_REJECTED, now);
		appendLog(approval, operatorId, "REJECT", comment);
		opLogService.log(operatorId, "APPROVAL_REJECT", "approval", approvalId, comment == null ? null : Map.of("comment", comment));
	}

	@Transactional
	public void revoke(long operatorId, long approvalId, String comment) {
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new ApiException(404, "未找到该审批"));
		if (!(STATUS_APPROVED.equals(approval.getStatus()) || STATUS_REJECTED.equals(approval.getStatus()))) {
			throw new ApiException(400, "该审批尚未被处理，无法撤回");
		}
		assigneeRepository.findOne(approvalId, operatorId)
				.orElseThrow(() -> new ApiException(403, "您不是该审批的指定审批人"));
		if (approval.getWindowExpireAt() != null && approval.getWindowExpireAt().isBefore(Instant.now())) {
			throw new ApiException(400, "撤回时间窗口已过期");
		}

		ApprovalStep step = stepRepository.findByApproval_IdOrderByStepNoAsc(approvalId).stream()
				.filter(s -> s.getStepNo() == 2)
				.findFirst()
				.orElseThrow(() -> new ApiException(500, "审批步骤缺失"));
		step.setStatus("PENDING");
		step.setActedBy(null);
		step.setActedAt(null);
		step.setComment(null);
		stepRepository.save(step);

		approval.setStatus(STATUS_PENDING);
		approval.setApprover(null);
		approvalRepository.save(approval);

		List<ApprovalAssignee> assignees = assigneeRepository.findByApprovalId(approvalId);
		for (ApprovalAssignee aa : assignees) {
			aa.setStatus(STATUS_PENDING);
			aa.setActedAt(null);
			aa.setComment(null);
			assigneeRepository.save(aa);
		}

		appendLog(approval, operatorId, STATUS_REVOKED, comment);
		opLogService.log(operatorId, "APPROVAL_REVOKE", "approval", approvalId, comment == null ? null : Map.of("comment", comment));
	}

	@Transactional
	public void revokeMyApproval(long applicantId, long approvalId) {
		Approval approval = approvalRepository.findById(approvalId)
				.orElseThrow(() -> new ApiException(404, "APPROVAL_NOT_FOUND"));
		if (approval.getApplicant() == null || approval.getApplicant().getId() != applicantId) {
			throw new ApiException(403, "NOT_YOUR_APPROVAL");
		}
		if (!STATUS_PENDING.equals(approval.getStatus())) {
			throw new ApiException(400, "CANNOT_REVOKE_PROCESSED");
		}

		ApprovalStep step = stepRepository.findByApproval_IdOrderByStepNoAsc(approvalId).stream()
				.filter(s -> s.getStepNo() == 2)
				.findFirst()
				.orElseThrow(() -> new ApiException(500, "STEP_MISSING"));
		step.setStatus("REVOKED");
		step.setActedBy(applicantId);
		step.setActedAt(Instant.now());
		step.setComment("学生主动撤回");
		stepRepository.save(step);

		approval.setStatus(STATUS_REVOKED);
		approvalRepository.save(approval);

		List<ApprovalAssignee> assignees = assigneeRepository.findByApprovalId(approvalId);
		for (ApprovalAssignee aa : assignees) {
			aa.setStatus(STATUS_REVOKED);
			aa.setActedAt(Instant.now());
			aa.setComment("学生主动撤回");
			assigneeRepository.save(aa);
		}

		if (TYPE_PARTY.equals(approval.getType()) || TYPE_LEAGUE.equals(approval.getType())) {
			progressService.onDecided(approval, STATUS_REVOKED, Instant.now());
		}

		appendLog(approval, applicantId, STATUS_REVOKED, "学生主动撤回");
		opLogService.log(applicantId, "APPROVAL_REVOKE_MY", "approval", approvalId, null);
	}

	private void appendLog(Approval approval, long operatorId, String action, String comment) {
		ApprovalLog log = new ApprovalLog();
		log.setApproval(approval);
		log.setOperatorId(operatorId);
		log.setAction(action);
		log.setComment(comment);
		logRepository.save(log);
	}

	private void saveAttachments(SysUser uploader, Approval approval, List<MultipartFile> files) {
		if (files == null || files.isEmpty()) {
			return;
		}
		try {
			Files.createDirectories(approvalDir);
		} catch (Exception e) {
			throw new ApiException(500, "STORAGE_INIT_FAILED");
		}
		for (MultipartFile f : files) {
			if (f == null || f.isEmpty()) {
				continue;
			}
			String originalName = f.getOriginalFilename() == null ? "attachment" : f.getOriginalFilename();
			String lowerName = originalName.toLowerCase(Locale.ROOT);
			if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".ppt") && !lowerName.endsWith(".pptx")
					&& !lowerName.endsWith(".doc") && !lowerName.endsWith(".docx") && !lowerName.endsWith(".txt")
					&& !lowerName.endsWith(".png") && !lowerName.endsWith(".jpg") && !lowerName.endsWith(".jpeg")) {
				throw new ApiException(400, "UNSUPPORTED_FILE_TYPE");
			}
			if (f.getSize() > 30L * 1024 * 1024) {
				throw new ApiException(400, "FILE_TOO_LARGE");
			}
			originalName = originalName.replace("\\", "/");
			int lastSlash = originalName.lastIndexOf('/');
			if (lastSlash >= 0) {
				originalName = originalName.substring(lastSlash + 1);
			}
			if (originalName.isBlank()) {
				originalName = "attachment";
			}
			String safeName = originalName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
			Path path = approvalDir.resolve(approval.getId() + "_" + Instant.now().toEpochMilli() + "_" + safeName);
			try {
				Files.copy(f.getInputStream(), path);
			} catch (Exception e) {
				throw new ApiException(500, "FILE_SAVE_FAILED");
			}

			ApprovalAttachment att = new ApprovalAttachment();
			att.setApproval(approval);
			att.setUploader(uploader);
			att.setFileName(originalName);
			att.setFilePath(path.toAbsolutePath().toString());
			att.setMimeType(f.getContentType());
			att.setFileSize(f.getSize());
			attachmentRepository.save(att);
		}
	}

	private static String str(Object v) {
		return v == null ? null : String.valueOf(v);
	}
}
