package com.schoolmanager.backend.security;

import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class SysUserDetailsService implements UserDetailsService {
	private final SysUserRepository userRepository;

	public SysUserDetailsService(SysUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		return userRepository.findByLoginName(username)
				.map(AuthUser::new)
				.orElseThrow(() -> new UsernameNotFoundException("USER_NOT_FOUND"));
	}
}
