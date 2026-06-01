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
@Table(name = "approval")
public class Approval {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "applicant_id", nullable = false)
	private SysUser applicant;

	@Column(name = "type", nullable = false, length = 32)
	private String type;

	@Column(name = "stage_code", length = 32)
	private String stageCode;

	@Column(name = "stage_index")
	private Integer stageIndex;

	@Column(name = "status", nullable = false, length = 16)
	private String status;

	@Column(name = "subject", length = 255)
	private String subject;

	@Column(name = "content", columnDefinition = "longtext")
	private String content;

	@Column(name = "current_step", nullable = false)
	private Integer currentStep = 0;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "approver_id")
	private SysUser approver;

	@Column(name = "window_expire_at")
	private Instant windowExpireAt;

	@Column(name = "form_json", columnDefinition = "json")
	private String formJson;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public SysUser getApplicant() {
		return applicant;
	}

	public void setApplicant(SysUser applicant) {
		this.applicant = applicant;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getStageCode() {
		return stageCode;
	}

	public void setStageCode(String stageCode) {
		this.stageCode = stageCode;
	}

	public Integer getStageIndex() {
		return stageIndex;
	}

	public void setStageIndex(Integer stageIndex) {
		this.stageIndex = stageIndex;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public Integer getCurrentStep() {
		return currentStep;
	}

	public void setCurrentStep(Integer currentStep) {
		this.currentStep = currentStep;
	}

	public SysUser getApprover() {
		return approver;
	}

	public void setApprover(SysUser approver) {
		this.approver = approver;
	}

	public Instant getWindowExpireAt() {
		return windowExpireAt;
	}

	public void setWindowExpireAt(Instant windowExpireAt) {
		this.windowExpireAt = windowExpireAt;
	}

	public String getFormJson() {
		return formJson;
	}

	public void setFormJson(String formJson) {
		this.formJson = formJson;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
