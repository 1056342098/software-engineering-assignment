package com.schoolmanager.backend.user;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
	private final SysUserRepository userRepository;

	public UserController(SysUserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@GetMapping
	public ApiResponse<List<UserDto>> listByRole(@RequestParam("role") @NotBlank String role) {
		List<SysUser> users = userRepository.findByRoleCode(role);
		return ApiResponse.ok(users.stream().map(UserDto::from).toList());
	}

	public record UserDto(Long id, String loginName, String realName) {
		static UserDto from(SysUser u) {
			return new UserDto(u.getId(), u.getLoginName(), u.getRealName());
		}
	}
}
