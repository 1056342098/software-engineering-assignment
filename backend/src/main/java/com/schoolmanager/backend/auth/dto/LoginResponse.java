package com.schoolmanager.backend.auth.dto;

import java.util.Set;

public class LoginResponse {
	private String token;
	private UserInfo user;

	public LoginResponse(String token, UserInfo user) {
		this.token = token;
		this.user = user;
	}

	public String getToken() {
		return token;
	}

	public UserInfo getUser() {
		return user;
	}

	public static class UserInfo {
		private Long id;
		private String loginName;
		private String realName;
		private Set<String> roles;

		public UserInfo(Long id, String loginName, String realName, Set<String> roles) {
			this.id = id;
			this.loginName = loginName;
			this.realName = realName;
			this.roles = roles;
		}

		public Long getId() {
			return id;
		}

		public String getLoginName() {
			return loginName;
		}

		public String getRealName() {
			return realName;
		}

		public Set<String> getRoles() {
			return roles;
		}
	}
}
