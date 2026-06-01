package com.schoolmanager.backend.policy;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.security.CurrentUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/policy")
public class PolicyController {
	private final PolicyService policyService;
	private final CurrentUser currentUser;

	public PolicyController(PolicyService policyService, CurrentUser currentUser) {
		this.policyService = policyService;
		this.currentUser = currentUser;
	}

	@GetMapping("/docs")
	public ApiResponse<List<PolicyDocDto>> list() {
		return ApiResponse.ok(policyService.listDocs().stream().map(PolicyDocDto::from).toList());
	}

	@GetMapping("/docs/mine")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<List<PolicyDocDto>> listMine() {
		return ApiResponse.ok(policyService.listMyDocs(currentUser.id()).stream().map(PolicyDocDto::from).toList());
	}

	@PostMapping(value = "/docs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<PolicyDocDto> upload(
			@RequestParam("title") @NotBlank String title,
			@RequestParam(value = "category", required = false) String category,
			@RequestParam("file") MultipartFile file) {
		PolicyDoc doc = policyService.upload(currentUser.id(), title, category, file);
		return ApiResponse.ok(PolicyDocDto.from(doc));
	}

	@PutMapping("/docs/{docId}")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<PolicyDocDto> update(@PathVariable long docId, @Valid @RequestBody UpdateReq req) {
		PolicyDoc doc = policyService.updateMeta(currentUser.id(), docId, req.title(), req.category());
		return ApiResponse.ok(PolicyDocDto.from(doc));
	}

	@DeleteMapping("/docs/{docId}")
	@PreAuthorize("hasAnyRole('TEACHER','LEADER')")
	public ApiResponse<Void> revoke(@PathVariable long docId) {
		policyService.revoke(currentUser.id(), docId);
		return ApiResponse.ok(null);
	}

	@GetMapping("/docs/{docId}/download")
	public ResponseEntity<Resource> download(@PathVariable long docId) {
		Resource file = policyService.getFile(docId);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"policy-" + docId + "\"")
				.body(file);
	}

	public record PolicyDocDto(Long id, String title, String category, String fileName, String status, Long uploaderId,
			String uploaderName) {
		public static PolicyDocDto from(PolicyDoc d) {
			Long uid = d.getUploader() == null ? null : d.getUploader().getId();
			String name = d.getUploader() == null ? null : d.getUploader().getRealName();
			return new PolicyDocDto(d.getId(), d.getTitle(), d.getCategory(), d.getFileName(), d.getStatus(), uid,
					name);
		}
	}

	public record UpdateReq(@NotNull @NotBlank String title, String category) {
	}
}
