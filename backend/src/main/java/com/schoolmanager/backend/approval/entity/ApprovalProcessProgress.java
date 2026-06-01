package com.schoolmanager.backend.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "approval_process_progress")
public class ApprovalProcessProgress {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "approval_type", nullable = false, length = 32)
	private String approvalType;

	@Column(name = "stage_index", nullable = false)
	private Integer stageIndex;

	@Column(name = "stage_code", nullable = false, length = 32)
	private String stageCode;

	@Column(name = "last_result", length = 16)
	private String lastResult;

	@Column(name = "last_assessed_at")
	private Instant lastAssessedAt;

	@Column(name = "last_approval_id")
	private Long lastApprovalId;

	@Column(name = "next_due_at")
	private Instant nextDueAt;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getApprovalType() {
		return approvalType;
	}

	public void setApprovalType(String approvalType) {
		this.approvalType = approvalType;
	}

	public Integer getStageIndex() {
		return stageIndex;
	}

	public void setStageIndex(Integer stageIndex) {
		this.stageIndex = stageIndex;
	}

	public String getStageCode() {
		return stageCode;
	}

	public void setStageCode(String stageCode) {
		this.stageCode = stageCode;
	}

	public String getLastResult() {
		return lastResult;
	}

	public void setLastResult(String lastResult) {
		this.lastResult = lastResult;
	}

	public Instant getLastAssessedAt() {
		return lastAssessedAt;
	}

	public void setLastAssessedAt(Instant lastAssessedAt) {
		this.lastAssessedAt = lastAssessedAt;
	}

	public Long getLastApprovalId() {
		return lastApprovalId;
	}

	public void setLastApprovalId(Long lastApprovalId) {
		this.lastApprovalId = lastApprovalId;
	}

	public Instant getNextDueAt() {
		return nextDueAt;
	}

	public void setNextDueAt(Instant nextDueAt) {
		this.nextDueAt = nextDueAt;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
