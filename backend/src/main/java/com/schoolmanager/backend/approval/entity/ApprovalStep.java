package com.schoolmanager.backend.approval.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "approval_step")
public class ApprovalStep {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approval_id", nullable = false)
	private Approval approval;

	@Column(name = "step_no", nullable = false)
	private Integer stepNo;

	@Column(name = "name", nullable = false, length = 64)
	private String name;

	@Column(name = "status", nullable = false, length = 16)
	private String status;

	@Column(name = "due_at")
	private Instant dueAt;

	@Column(name = "acted_by")
	private Long actedBy;

	@Column(name = "acted_at")
	private Instant actedAt;

	@Column(name = "comment", length = 255)
	private String comment;

	public Long getId() {
		return id;
	}

	public Approval getApproval() {
		return approval;
	}

	public void setApproval(Approval approval) {
		this.approval = approval;
	}

	public Integer getStepNo() {
		return stepNo;
	}

	public void setStepNo(Integer stepNo) {
		this.stepNo = stepNo;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Instant getDueAt() {
		return dueAt;
	}

	public void setDueAt(Instant dueAt) {
		this.dueAt = dueAt;
	}

	public Long getActedBy() {
		return actedBy;
	}

	public void setActedBy(Long actedBy) {
		this.actedBy = actedBy;
	}

	public Instant getActedAt() {
		return actedAt;
	}

	public void setActedAt(Instant actedAt) {
		this.actedAt = actedAt;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}
}
