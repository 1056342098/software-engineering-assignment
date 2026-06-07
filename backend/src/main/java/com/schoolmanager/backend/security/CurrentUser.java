package com.schoolmanager.backend.security;

import com.schoolmanager.backend.common.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class CurrentUser {
	public long id() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof AuthUser au)) {
			throw new ApiException(401, "未登录或登录已过期");
		}
		return au.getUser().getId();
	}

	public Set<String> roleCodes() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth == null || !(auth.getPrincipal() instanceof AuthUser au)) {
			throw new ApiException(401, "未登录或登录已过期");
		}
		return au.getUser().getRoles().stream().map(r -> r.getCode()).collect(java.util.stream.Collectors.toSet());
	}

	public boolean hasRole(String roleCode) {
		return roleCodes().contains(roleCode);
	}
}
