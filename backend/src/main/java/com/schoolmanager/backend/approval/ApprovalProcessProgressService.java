package com.schoolmanager.backend.approval;

import com.schoolmanager.backend.approval.entity.Approval;
import com.schoolmanager.backend.approval.entity.ApprovalProcessProgress;
import com.schoolmanager.backend.approval.repo.ApprovalProcessProgressRepository;
import com.schoolmanager.backend.approval.repo.ApprovalRepository;
import com.schoolmanager.backend.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class ApprovalProcessProgressService {
	public static final int CYCLE_DAYS = 0;

	public record StageDef(String code, String name) {
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

	public ApprovalProcessProgressService(
			ApprovalRepository approvalRepository,
			ApprovalProcessProgressRepository progressRepository) {
		this.approvalRepository = approvalRepository;
		this.progressRepository = progressRepository;
	}

	@Transactional
	public SubmissionStage prepareSubmission(long userId, String approvalType, Instant now) {
		List<StageDef> stages = stagesForType(approvalType);
		if (stages == null) {
			return new SubmissionStage(0, null);
		}

		ApprovalProcessProgress p = ensureProgress(userId, approvalType, now);
		if (isFinalStage(p, stages)) {
			throw new ApiException(400, "ALREADY_FINAL_STAGE");
		}
		if (p.getNextDueAt() != null && now.isBefore(p.getNextDueAt())) {
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
		if (stages == null) {
			return;
		}
		Instant now = Instant.now();
		ApprovalProcessProgress p = ensureProgress(approval.getApplicant().getId(), approval.getType(), now);
		p.setLastApprovalId(approval.getId());
		p.setLastResult(ApprovalService.STATUS_PENDING);
		p.setNextDueAt(null);
		progressRepository.save(p);
	}

	@Transactional
	public void onDecided(Approval approval, String decision, Instant decidedAt) {
		List<StageDef> stages = stagesForType(approval.getType());
		if (stages == null) {
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
			p.setNextDueAt(stageIndex >= stages.size() - 1 ? null : decidedAt.plus(CYCLE_DAYS, ChronoUnit.DAYS));
		} else if (ApprovalService.STATUS_REJECTED.equals(decision)) {
			p.setNextDueAt(decidedAt.plus(CYCLE_DAYS, ChronoUnit.DAYS));
		} else if (ApprovalService.STATUS_REVOKED.equals(decision)) {
			// When revoked, just keep the current nextDueAt or reset properly if needed.
			// The important part is clearing the pendingApprovalId which is done dynamically by the getView method
			// since it queries for STATUS_PENDING applications.
			// Let's also ensure the lastResult reflects the revocation.
		}

		p.setLastApprovalId(approval.getId());
		p.setLastResult(decision);
		p.setLastAssessedAt(decidedAt);
		progressRepository.save(p);
	}

	@Transactional(readOnly = true)
	public ProgressView getView(long userId, String approvalType) {
		List<StageDef> stages = stagesForType(approvalType);
		if (stages == null) {
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

		return new ProgressView(
				approvalType,
				idx,
				code,
				stageName,
				stages,
				p.getLastResult(),
				p.getLastAssessedAt(),
				p.getNextDueAt(),
				p.getLastApprovalId(),
				pendingId
		);
	}

	private ApprovalProcessProgress ensureProgress(long userId, String approvalType, Instant now) {
		List<StageDef> stages = stagesForType(approvalType);
		if (stages == null) {
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

	private static List<StageDef> stagesForType(String approvalType) {
		String t = approvalType == null ? "" : approvalType.trim().toUpperCase(Locale.ROOT);
		if (ApprovalService.TYPE_PARTY.equals(t)) {
			return List.of(
					new StageDef("APPLICANT", "入党申请人"),
					new StageDef("ACTIVE", "积极分子"),
					new StageDef("DEVELOPMENT", "发展对象"),
					new StageDef("PROBATIONARY", "预备党员"),
					new StageDef("FULL", "正式党员")
			);
		}
		if (ApprovalService.TYPE_LEAGUE.equals(t)) {
			return List.of(
					new StageDef("APPLICANT", "入团申请"),
					new StageDef("PROBATIONARY", "预备团员"),
					new StageDef("FULL", "正式团员")
			);
		}
		return null;
	}
}
