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
@Table(name = "notification_email_config")
public class NotificationEmailConfig {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "user_id", nullable = false)
	private SysUser user;

	@Column(name = "sender_email", nullable = false, length = 128)
	private String senderEmail;

	@Column(name = "sender_name", length = 64)
	private String senderName;

	@Column(name = "smtp_host", nullable = false, length = 255)
	private String smtpHost;

	@Column(name = "smtp_port", nullable = false)
	private Integer smtpPort;

	@Column(name = "smtp_username", nullable = false, length = 255)
	private String smtpUsername;

	@Column(name = "smtp_password_enc", nullable = false, length = 512)
	private String smtpPasswordEnc;

	@Column(name = "starttls_enabled", nullable = false)
	private boolean starttlsEnabled = true;

	@Column(name = "ssl_enabled", nullable = false)
	private boolean sslEnabled;

	@Column(name = "created_at", insertable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public SysUser getUser() {
		return user;
	}

	public void setUser(SysUser user) {
		this.user = user;
	}

	public String getSenderEmail() {
		return senderEmail;
	}

	public void setSenderEmail(String senderEmail) {
		this.senderEmail = senderEmail;
	}

	public String getSenderName() {
		return senderName;
	}

	public void setSenderName(String senderName) {
		this.senderName = senderName;
	}

	public String getSmtpHost() {
		return smtpHost;
	}

	public void setSmtpHost(String smtpHost) {
		this.smtpHost = smtpHost;
	}

	public Integer getSmtpPort() {
		return smtpPort;
	}

	public void setSmtpPort(Integer smtpPort) {
		this.smtpPort = smtpPort;
	}

	public String getSmtpUsername() {
		return smtpUsername;
	}

	public void setSmtpUsername(String smtpUsername) {
		this.smtpUsername = smtpUsername;
	}

	public String getSmtpPasswordEnc() {
		return smtpPasswordEnc;
	}

	public void setSmtpPasswordEnc(String smtpPasswordEnc) {
		this.smtpPasswordEnc = smtpPasswordEnc;
	}

	public boolean isStarttlsEnabled() {
		return starttlsEnabled;
	}

	public void setStarttlsEnabled(boolean starttlsEnabled) {
		this.starttlsEnabled = starttlsEnabled;
	}

	public boolean isSslEnabled() {
		return sslEnabled;
	}

	public void setSslEnabled(boolean sslEnabled) {
		this.sslEnabled = sslEnabled;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
