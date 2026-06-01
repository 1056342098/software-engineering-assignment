package com.schoolmanager.backend.student.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "class_manager")
@IdClass(ClassManager.Pk.class)
public class ClassManager {
	@Id
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Id
	@Column(name = "class_name", nullable = false, length = 64)
	private String className;

	public Long getUserId() {
		return userId;
	}

	public void setUserId(Long userId) {
		this.userId = userId;
	}

	public String getClassName() {
		return className;
	}

	public void setClassName(String className) {
		this.className = className;
	}

	public static class Pk implements Serializable {
		private Long userId;
		private String className;

		public Pk() {
		}

		public Pk(Long userId, String className) {
			this.userId = userId;
			this.className = className;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;
			Pk pk = (Pk) o;
			return Objects.equals(userId, pk.userId) && Objects.equals(className, pk.className);
		}

		@Override
		public int hashCode() {
			return Objects.hash(userId, className);
		}
	}
}
