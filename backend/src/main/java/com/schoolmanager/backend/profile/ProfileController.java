package com.schoolmanager.backend.profile;

import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    private final ProfileService profileService;
    private final CurrentUser currentUser;

    public ProfileController(ProfileService profileService, CurrentUser currentUser) {
        this.profileService = profileService;
        this.currentUser = currentUser;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        long uid = currentUser.id();
        boolean isAdmin = currentUser.hasRole("LEADER") || currentUser.hasRole("TEACHER");
        if (currentUser.hasRole("STUDENT")) {
            return ApiResponse.ok(profileService.getProfile(uid, isAdmin, uid));
        }
        return ApiResponse.ok(profileService.getUserProfile(uid, isAdmin, uid));
    }

    @GetMapping("/students/{studentId}")
    @PreAuthorize("hasAnyRole('LEADER','TEACHER','CADRE')")
    public ApiResponse<Map<String, Object>> getStudent(@PathVariable long studentId) {
        long uid = currentUser.id();
        boolean isAdmin = currentUser.hasRole("LEADER") || currentUser.hasRole("TEACHER");
        return ApiResponse.ok(profileService.getProfile(uid, isAdmin, studentId));
    }

    @PutMapping("/me/public")
    public ApiResponse<Void> updateMyPublic(@NotNull @RequestBody Map<String, Object> body) {
        profileService.upsertPublicProfile(currentUser.id(), body);
        return ApiResponse.ok(null);
    }

    @PutMapping("/students/{studentId}/public")
    @PreAuthorize("hasAnyRole('LEADER','TEACHER')")
    public ApiResponse<Void> updateStudentPublic(@PathVariable long studentId,
            @NotNull @RequestBody Map<String, Object> body) {
        profileService.upsertStudentPublicProfile(currentUser.id(), studentId, body);
        return ApiResponse.ok(null);
    }

    @PutMapping("/students/{studentId}/sensitive")
    public ApiResponse<Void> updateSensitive(@PathVariable long studentId,
            @NotNull @RequestBody Map<String, Object> body) {
        long uid = currentUser.id();
        boolean isAdmin = currentUser.hasRole("LEADER") || currentUser.hasRole("TEACHER");
        if (!(isAdmin || uid == studentId)) {
            throw new ApiException(403, "没有权限修改敏感信息");
        }
        profileService.upsertSensitive(uid, studentId, body);
        return ApiResponse.ok(null);
    }
}
