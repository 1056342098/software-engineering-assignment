package com.schoolmanager.backend.notification.entity;

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
@Table(name = "notification_delivery")
public class NotificationDelivery {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "recipient_id", nullable = false)
	private NotificationRecipient recipient;

	@Column(name = "channel", nullable = false, length = 16)
	private String channel;

	@Column(name = "status", nullable = false, length = 16)
	private String status;

	@Column(name = "provider_message", length = 255)
	private String providerMessage;

	@Column(name = "sent_at")
	private Instant sentAt;

	public Long getId() {
		return id;
	}

	public NotificationRecipient getRecipient() {
		return recipient;
	}

	public void setRecipient(NotificationRecipient recipient) {
		this.recipient = recipient;
	}

	public String getChannel() {
		return channel;
	}

	public void setChannel(String channel) {
		this.channel = channel;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getProviderMessage() {
		return providerMessage;
	}

	public void setProviderMessage(String providerMessage) {
		this.providerMessage = providerMessage;
	}

	public Instant getSentAt() {
		return sentAt;
	}

	public void setSentAt(Instant sentAt) {
		this.sentAt = sentAt;
	}
}
