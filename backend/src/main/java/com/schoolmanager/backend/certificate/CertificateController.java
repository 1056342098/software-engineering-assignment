package com.schoolmanager.backend.certificate;

import com.schoolmanager.backend.certificate.entity.CertificateTemplate;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.profile.ProfileService;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/certificates")
public class CertificateController {

    private final CertificateTemplateService templateService;
    private final ProfileService profileService;
    private final CurrentUser currentUser;

    public CertificateController(CertificateTemplateService templateService, ProfileService profileService, CurrentUser currentUser) {
        this.templateService = templateService;
        this.profileService = profileService;
        this.currentUser = currentUser;
    }

    // --- Template Management ---

    @GetMapping("/templates")
    public ApiResponse<List<CertificateTemplate>> listTemplates() {
        return ApiResponse.ok(templateService.listAll());
    }

    @GetMapping("/templates/enabled")
    public ApiResponse<List<CertificateTemplate>> listEnabledTemplates() {
        return ApiResponse.ok(templateService.listEnabled());
    }

    public record CreateTemplateReq(@NotBlank String name, @NotBlank String content) {}

    @PostMapping("/templates")
    @PreAuthorize("hasAnyRole('LEADER','TEACHER')")
    public ApiResponse<CertificateTemplate> createTemplate(@Valid @RequestBody CreateTemplateReq req) {
        return ApiResponse.ok(templateService.create(req.name(), req.content()));
    }

    public record UpdateTemplateReq(String name, String content, Boolean enabled) {}

    @PostMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('LEADER','TEACHER')")
    public ApiResponse<CertificateTemplate> updateTemplate(@PathVariable long id, @RequestBody UpdateTemplateReq req) {
        return ApiResponse.ok(templateService.update(id, req.name(), req.content(), req.enabled()));
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAnyRole('LEADER','TEACHER')")
    public ApiResponse<Void> deleteTemplate(@PathVariable long id) {
        templateService.delete(id);
        return ApiResponse.ok(null);
    }

    // --- Certificate Generation ---

    @GetMapping("/preview/{templateId}")
    public ApiResponse<String> previewCertificate(@PathVariable long templateId) {
        CertificateTemplate template = templateService.listEnabled().stream()
                .filter(t -> t.getId() == templateId)
                .findFirst()
                .orElseThrow(() -> new ApiException(404, "未找到可用的模板"));

        long uid = currentUser.id();
        boolean isAdmin = currentUser.hasRole("LEADER") || currentUser.hasRole("TEACHER");
        Map<String, Object> profile = profileService.getProfile(uid, isAdmin, uid);

        String content = template.getContent();
        if (profile != null) {
            content = content.replace("${realName}", strVal(profile.get("realName")));
            content = content.replace("${studentNo}", strVal(profile.get("studentNo")));
            content = content.replace("${major}", strVal(profile.get("major")));
            content = content.replace("${grade}", strVal(profile.get("grade")));
            content = content.replace("${className}", strVal(profile.get("className")));
        }

        return ApiResponse.ok(content);
    }

    private String strVal(Object obj) {
        return obj == null ? "" : obj.toString();
    }
}
