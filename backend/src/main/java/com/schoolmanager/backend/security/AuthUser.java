package com.schoolmanager.backend.security;

import com.schoolmanager.backend.user.entity.SysRole;
import com.schoolmanager.backend.user.entity.SysUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class AuthUser implements UserDetails {
	private final SysUser user;

	public AuthUser(SysUser user) {
		this.user = user;
	}

	public SysUser getUser() {
		return user;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		List<SimpleGrantedAuthority> authorities = user.getRoles().stream()
				.map(SysRole::getCode)
				.map(code -> new SimpleGrantedAuthority("ROLE_" + code))
				.toList();
		return authorities;
	}

	@Override
	public String getPassword() {
		return user.getPasswordHash();
	}

	@Override
	public String getUsername() {
		return user.getLoginName();
	}

	@Override
	public boolean isAccountNonExpired() {
		return true;
	}

	@Override
	public boolean isAccountNonLocked() {
		return user.getStatus() != null && user.getStatus() != (short) 2;
	}

	@Override
	public boolean isCredentialsNonExpired() {
		return true;
	}

	@Override
	public boolean isEnabled() {
		return user.getStatus() != null && user.getStatus() == (short) 1;
	}
}
