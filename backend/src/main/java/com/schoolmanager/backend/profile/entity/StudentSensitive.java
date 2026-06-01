package com.schoolmanager.backend.profile.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "student_sensitive")
public class StudentSensitive {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne
	@JoinColumn(name = "student_id", nullable = false)
	private Student student;

	@Column(name = "id_card_no_enc", length = 255)
	private String idCardNoEnc;

	@Column(name = "hukou_addr_enc", length = 255)
	private String hukouAddrEnc;

	@Column(name = "hometown_enc", length = 255)
	private String hometownEnc;

	@Column(name = "tutor_enc", length = 255)
	private String tutorEnc;

	@Column(name = "delay_info_enc", columnDefinition = "text")
	private String delayInfoEnc;

	@Column(name = "updated_by")
	private Long updatedBy;

	@Column(name = "updated_at", insertable = false, updatable = false)
	private Instant updatedAt;

	public Long getId() {
		return id;
	}

	public Student getStudent() {
		return student;
	}

	public void setStudent(Student student) {
		this.student = student;
	}

	public String getIdCardNoEnc() {
		return idCardNoEnc;
	}

	public void setIdCardNoEnc(String idCardNoEnc) {
		this.idCardNoEnc = idCardNoEnc;
	}

	public String getHukouAddrEnc() {
		return hukouAddrEnc;
	}

	public void setHukouAddrEnc(String hukouAddrEnc) {
		this.hukouAddrEnc = hukouAddrEnc;
	}

	public String getHometownEnc() {
		return hometownEnc;
	}

	public void setHometownEnc(String hometownEnc) {
		this.hometownEnc = hometownEnc;
	}

	public String getTutorEnc() {
		return tutorEnc;
	}

	public void setTutorEnc(String tutorEnc) {
		this.tutorEnc = tutorEnc;
	}

	public String getDelayInfoEnc() {
		return delayInfoEnc;
	}

	public void setDelayInfoEnc(String delayInfoEnc) {
		this.delayInfoEnc = delayInfoEnc;
	}

	public Long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(Long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
