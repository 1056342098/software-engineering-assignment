package com.schoolmanager.backend.notification.entity;

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
@Table(name = "notification")
public class Notification {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "title", nullable = false, length = 255)
	private String title;

	@Column(name = "content", nullable = false, columnDefinition = "text")
	private String content;

	@Column(name = "attachment_name", length = 255)
	private String attachmentName;

	@Column(name = "attachment_url", length = 1024)
	private String attachmentUrl;

	@Column(name = "attachment_file_path", length = 1024)
	private String attachmentFilePath;

	@Column(name = "attachment_mime_type", length = 128)
	private String attachmentMimeType;

	@Column(name = "attachment_file_size")
	private Long attachmentFileSize;

	@Column(name = "expire_at")
	private Instant expireAt;

	@Column(name = "tags_json", columnDefinition = "json")
	private String tagsJson;

	@Column(name = "channels_json", nullable = false, columnDefinition = "json")
	private String channelsJson;

	@Column(name = "target_json", columnDefinition = "json")
	private String targetJson;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "created_by", nullable = false)
	private SysUser createdBy;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getAttachmentName() {
		return attachmentName;
	}

	public void setAttachmentName(String attachmentName) {
		this.attachmentName = attachmentName;
	}

	public String getAttachmentUrl() {
		return attachmentUrl;
	}

	public void setAttachmentUrl(String attachmentUrl) {
		this.attachmentUrl = attachmentUrl;
	}

	public String getAttachmentFilePath() {
		return attachmentFilePath;
	}

	public void setAttachmentFilePath(String attachmentFilePath) {
		this.attachmentFilePath = attachmentFilePath;
	}

	public String getAttachmentMimeType() {
		return attachmentMimeType;
	}

	public void setAttachmentMimeType(String attachmentMimeType) {
		this.attachmentMimeType = attachmentMimeType;
	}

	public Long getAttachmentFileSize() {
		return attachmentFileSize;
	}

	public void setAttachmentFileSize(Long attachmentFileSize) {
		this.attachmentFileSize = attachmentFileSize;
	}

	public Instant getExpireAt() {
		return expireAt;
	}

	public void setExpireAt(Instant expireAt) {
		this.expireAt = expireAt;
	}

	public String getTagsJson() {
		return tagsJson;
	}

	public void setTagsJson(String tagsJson) {
		this.tagsJson = tagsJson;
	}

	public String getChannelsJson() {
		return channelsJson;
	}

	public void setChannelsJson(String channelsJson) {
		this.channelsJson = channelsJson;
	}

	public String getTargetJson() {
		return targetJson;
	}

	public void setTargetJson(String targetJson) {
		this.targetJson = targetJson;
	}

	public SysUser getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(SysUser createdBy) {
		this.createdBy = createdBy;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
