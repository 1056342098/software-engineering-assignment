package com.schoolmanager.backend.oplog.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "operation_log")
public class OperationLog {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "operator_id", nullable = false)
	private Long operatorId;

	@Column(name = "action", nullable = false, length = 64)
	private String action;

	@Column(name = "target_type", length = 64)
	private String targetType;

	@Column(name = "target_id")
	private Long targetId;

	@Column(name = "detail_json", columnDefinition = "json")
	private String detailJson;

	@Column(name = "ts", insertable = false, updatable = false)
	private Instant ts;

	public Long getId() {
		return id;
	}

	public Long getOperatorId() {
		return operatorId;
	}

	public void setOperatorId(Long operatorId) {
		this.operatorId = operatorId;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public String getTargetType() {
		return targetType;
	}

	public void setTargetType(String targetType) {
		this.targetType = targetType;
	}

	public Long getTargetId() {
		return targetId;
	}

	public void setTargetId(Long targetId) {
		this.targetId = targetId;
	}

	public String getDetailJson() {
		return detailJson;
	}

	public void setDetailJson(String detailJson) {
		this.detailJson = detailJson;
	}

	public Instant getTs() {
		return ts;
	}
}
