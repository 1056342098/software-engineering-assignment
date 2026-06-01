package com.schoolmanager.backend.approval.entity;

import com.schoolmanager.backend.user.entity.SysUser;
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
@Table(name = "approval_assignee")
public class ApprovalAssignee {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approval_id", nullable = false)
	private Approval approval;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approver_id", nullable = false)
	private SysUser approver;

	@Column(name = "status", nullable = false, length = 16)
	private String status;

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

	public SysUser getApprover() {
		return approver;
	}

	public void setApprover(SysUser approver) {
		this.approver = approver;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
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

