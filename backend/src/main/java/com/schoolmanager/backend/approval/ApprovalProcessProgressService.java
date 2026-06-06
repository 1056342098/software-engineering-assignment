package com.schoolmanager.backend.approval;

import com.schoolmanager.backend.approval.entity.Approval;
import com.schoolmanager.backend.approval.entity.ApprovalProcessProgress;
import com.schoolmanager.backend.approval.entity.ProcessTimelineNode;
import com.schoolmanager.backend.approval.repo.ApprovalProcessProgressRepository;
import com.schoolmanager.backend.approval.repo.ApprovalRepository;
import com.schoolmanager.backend.approval.repo.ProcessTimelineNodeRepository;
import com.schoolmanager.backend.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApprovalProcessProgressService {

	public record StageDef(String code, String name, int intervalDays) {
	}

	public record ProgressView(
			String approvalType,
			int stageIndex,
			String stageCode,
			String stageName,
			List<StageDef> stages,
			String lastResult,
			Instant lastAssessedAt,
			Instant nextDueAt,
			Long lastApprovalId,
			Long pendingApprovalId
	) {
	}

	public record SubmissionStage(int stageIndex, String stageCode) {
	}

	private final ApprovalRepository approvalRepository;
	private final ApprovalProcessProgressRepository progressRepository;
	private final ProcessTimelineNodeRepository timelineNodeRepository;

	public ApprovalProcessProgressService(
			ApprovalRepository approvalRepository,
			ApprovalProcessProgressRepository progressRepository,
			ProcessTimelineNodeRepository timelineNodeRepository) {
		this.approvalRepository = approvalRepository;
		this.progressRepository = progressRepository;
		this.timelineNodeRepository = timelineNodeRepository;
	}

	private Instant calculateNextDueAt(ApprovalProcessProgress p, List<StageDef> stages) {
		int idx = clampStageIndex(p.getStageIndex(), stages);
		if (idx >= stages.size() - 1) {
			return null;
		}
		int interval = stages.get(idx).intervalDays();
		if (p.getLastAssessedAt() != null) {
			return p.getLastAssessedAt().plus(interval, ChronoUnit.DAYS);
		} else if (p.getCreatedAt() != null) {
			return p.getCreatedAt().plus(interval, ChronoUnit.DAYS);
		} else {
			return Instant.now().plus(interval, ChronoUnit.DAYS);
		}
	}

	@Transactional
	public SubmissionStage prepareSubmission(long userId, String approvalType, Instant now) {
		List<StageDef> stages = stagesForType(approvalType);
		if (stages.isEmpty()) {
			return new SubmissionStage(0, null);
		}

		ApprovalProcessProgress p = ensureProgress(userId, approvalType, now);
		if (isFinalStage(p, stages)) {
			throw new ApiException(400, "ALREADY_FINAL_STAGE");
		}
		Instant actualNextDueAt = calculateNextDueAt(p, stages);
		if (actualNextDueAt != null && now.isBefore(actualNextDueAt)) {
			throw new ApiException(400, "ASSESSMENT_NOT_DUE");
		}
		if (approvalRepository.existsByApplicant_IdAndTypeAndStatus(userId, approvalType, ApprovalService.STATUS_PENDING)) {
			throw new ApiException(400, "ASSESSMENT_PENDING");
		}
		return new SubmissionStage(p.getStageIndex(), p.getStageCode());
	}

	@Transactional
	public void onSubmitted(Approval approval) {
		List<StageDef> stages = stagesForType(approval.getType());
		if (stages.isEmpty()) {
			return;
		}
		Instant now = Instant.now();
		ApprovalProcessProgress p = ensureProgress(approval.getApplicant().getId(), approval.getType(), now);
		p.setLastApprovalId(approval.getId());
		p.setLastResult(ApprovalService.STATUS_PENDING);
		progressRepository.save(p);
	}

	@Transactional
	public void onDecided(Approval approval, String decision, Instant decidedAt) {
		List<StageDef> stages = stagesForType(approval.getType());
		if (stages.isEmpty()) {
			return;
		}
		ApprovalProcessProgress p = ensureProgress(approval.getApplicant().getId(), approval.getType(), decidedAt);

		int stageIndex = approval.getStageIndex() == null ? p.getStageIndex() : approval.getStageIndex();
		stageIndex = clampStageIndex(stageIndex, stages);
		p.setStageIndex(stageIndex);
		p.setStageCode(stages.get(stageIndex).code());

		if (ApprovalService.STATUS_APPROVED.equals(decision)) {
			if (stageIndex < stages.size() - 1) {
				stageIndex += 1;
				p.setStageIndex(stageIndex);
				p.setStageCode(stages.get(stageIndex).code());
			}
			int nextInterval = stages.get(stageIndex).intervalDays();
			p.setNextDueAt(stageIndex >= stages.size() - 1 ? null : decidedAt.plus(nextInterval, ChronoUnit.DAYS));
		} else if (ApprovalService.STATUS_REJECTED.equals(decision)) {
			int currentInterval = stages.get(stageIndex).intervalDays();
			p.setNextDueAt(decidedAt.plus(currentInterval, ChronoUnit.DAYS));
		} else if (ApprovalService.STATUS_REVOKED.equals(decision)) {
			// When revoked, just keep the current nextDueAt or reset properly if needed.
		}

		p.setLastApprovalId(approval.getId());
		p.setLastResult(decision);
		p.setLastAssessedAt(decidedAt);
		progressRepository.save(p);
	}

	@Transactional(readOnly = true)
	public ProgressView getView(long userId, String approvalType) {
		List<StageDef> stages = stagesForType(approvalType);
		if (stages.isEmpty()) {
			throw new ApiException(400, "INVALID_TYPE");
		}
		ApprovalProcessProgress p = progressRepository.findByUserIdAndApprovalType(userId, approvalType)
				.orElseGet(() -> {
					ApprovalProcessProgress created = new ApprovalProcessProgress();
					created.setUserId(userId);
					created.setApprovalType(approvalType);
					created.setStageIndex(0);
					created.setStageCode(stages.get(0).code());
					created.setNextDueAt(Instant.now());
					return created;
				});

		Long pendingId = approvalRepository
				.findFirstByApplicant_IdAndTypeAndStatusOrderByIdDesc(userId, approvalType, ApprovalService.STATUS_PENDING)
				.map(Approval::getId)
				.orElse(null);

		int idx = clampStageIndex(p.getStageIndex(), stages);
		String code = p.getStageCode();
		if (code == null || code.isBlank() || !code.equalsIgnoreCase(stages.get(idx).code())) {
			code = stages.get(idx).code();
		}
		String stageName = stages.get(idx).name();

		Instant nextDueAt = calculateNextDueAt(p, stages);

		return new ProgressView(
				approvalType,
				idx,
				code,
				stageName,
				stages,
				p.getLastResult(),
				p.getLastAssessedAt(),
				nextDueAt,
				p.getLastApprovalId(),
				pendingId
		);
	}

	private ApprovalProcessProgress ensureProgress(long userId, String approvalType, Instant now) {
		List<StageDef> stages = stagesForType(approvalType);
		if (stages.isEmpty()) {
			throw new ApiException(400, "INVALID_TYPE");
		}
		Optional<ApprovalProcessProgress> op = progressRepository.findByUserIdAndApprovalType(userId, approvalType);
		if (op.isPresent()) {
			return op.get();
		}
		ApprovalProcessProgress created = new ApprovalProcessProgress();
		created.setUserId(userId);
		created.setApprovalType(approvalType);
		created.setStageIndex(0);
		created.setStageCode(stages.get(0).code());
		created.setNextDueAt(now);
		return progressRepository.save(created);
	}

	private static boolean isFinalStage(ApprovalProcessProgress p, List<StageDef> stages) {
		int idx = p.getStageIndex() == null ? 0 : p.getStageIndex();
		return idx >= stages.size() - 1;
	}

	private static int clampStageIndex(Integer idx, List<StageDef> stages) {
		int i = idx == null ? 0 : idx;
		if (i < 0) i = 0;
		if (i >= stages.size()) i = stages.size() - 1;
		return i;
	}

	private List<StageDef> stagesForType(String approvalType) {
		String t = approvalType == null ? "" : approvalType.trim().toUpperCase(Locale.ROOT);
		List<ProcessTimelineNode> nodes = timelineNodeRepository.findByApprovalTypeOrderByStageIndexAsc(t);
		return nodes.stream()
				.map(n -> new StageDef(n.getStageCode(), n.getStageName(), n.getIntervalDays()))
				.collect(Collectors.toList());
	}
}
