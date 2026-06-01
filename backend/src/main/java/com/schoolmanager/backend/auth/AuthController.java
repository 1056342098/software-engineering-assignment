package com.schoolmanager.backend.auth;

import com.schoolmanager.backend.auth.dto.LoginRequest;
import com.schoolmanager.backend.auth.dto.LoginResponse;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.security.AuthUser;
import com.schoolmanager.backend.security.JwtService;
import com.schoolmanager.backend.user.entity.SysRole;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import jakarta.validation.Valid;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final SysUserRepository userRepository;

    public AuthController(AuthenticationManager authenticationManager, JwtService jwtService,
            SysUserRepository userRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getLoginName(), req.getPassword()));
        AuthUser principal = (AuthUser) auth.getPrincipal();
        SysUser u = principal.getUser();

        Set<String> roles = u.getRoles().stream().map(SysRole::getCode).collect(java.util.stream.Collectors.toSet());
        String token = jwtService.issueToken(u.getId(), u.getLoginName(), roles);
        return ApiResponse.ok(new LoginResponse(token,
                new LoginResponse.UserInfo(u.getId(), u.getLoginName(), u.getRealName(), roles)));
    }

    @GetMapping("/me")
    public ApiResponse<LoginResponse.UserInfo> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AuthUser au)) {
            throw new ApiException(401, "UNAUTHORIZED");
        }
        SysUser u = userRepository.findById(au.getUser().getId())
                .orElseThrow(() -> new ApiException(404, "USER_NOT_FOUND"));
        Set<String> roles = u.getRoles().stream().map(SysRole::getCode).collect(java.util.stream.Collectors.toSet());
        return ApiResponse.ok(new LoginResponse.UserInfo(u.getId(), u.getLoginName(), u.getRealName(), roles));
    }
}
