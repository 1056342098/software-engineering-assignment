package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.policy.repo.PolicyDocChunkRepository;
import com.schoolmanager.backend.policy.repo.PolicyDocRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/qa")
public class QaController {
	private final PolicyDocChunkRepository chunkRepository;
	private final PolicyDocRepository docRepository;

	public QaController(PolicyDocChunkRepository chunkRepository, PolicyDocRepository docRepository) {
		this.chunkRepository = chunkRepository;
		this.docRepository = docRepository;
	}

	@PostMapping("/ask")
	public ApiResponse<AskResponse> ask(@Valid @RequestBody AskRequest req) {
		int limit = Math.min(Math.max(req.getTopK() == null ? 5 : req.getTopK(), 1), 10);
		List<PolicyDocChunkRepository.PolicyChunkSearchRow> hits = chunkRepository.searchTop(req.getQuestion())
				.stream()
				.limit(limit)
				.toList();
		if (hits.isEmpty()) {
			return ApiResponse.ok(new AskResponse("未找到相关政策条目，请尝试更换关键词。", null));
		}
		var best = hits.get(0);
		var doc = docRepository.findById(best.getDocId()).orElse(null);
		Source source = doc == null ? null : new Source(doc.getId(), doc.getTitle(), best.getChunkNo());
		return ApiResponse.ok(new AskResponse(best.getChunkText(), source));
	}

	public static class AskRequest {
		@NotBlank
		private String question;
		private Integer topK;

		public String getQuestion() {
			return question;
		}

		public void setQuestion(String question) {
			this.question = question;
		}

		public Integer getTopK() {
			return topK;
		}

		public void setTopK(Integer topK) {
			this.topK = topK;
		}
	}

	public record AskResponse(String answer, Source source) {
	}

	public record Source(Long docId, String title, Integer chunkNo) {
	}
}
