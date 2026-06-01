package com.schoolmanager.backend.policy.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_doc_chunk")
public class PolicyDocChunk {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "doc_id", nullable = false)
	private PolicyDoc doc;

	@Column(name = "chunk_no", nullable = false)
	private Integer chunkNo;

	@Column(name = "chunk_text", nullable = false, columnDefinition = "longtext")
	private String chunkText;

	public Long getId() {
		return id;
	}

	public PolicyDoc getDoc() {
		return doc;
	}

	public void setDoc(PolicyDoc doc) {
		this.doc = doc;
	}

	public Integer getChunkNo() {
		return chunkNo;
	}

	public void setChunkNo(Integer chunkNo) {
		this.chunkNo = chunkNo;
	}

	public String getChunkText() {
		return chunkText;
	}

	public void setChunkText(String chunkText) {
		this.chunkText = chunkText;
	}
}
