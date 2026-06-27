package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.common.ApiResponse;
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
	private final QaService qaService;

	public QaController(QaService qaService) {
		this.qaService = qaService;
	}

	@PostMapping("/ask")
	public ApiResponse<AskResponse> ask(@Valid @RequestBody AskRequest req) {
		QaService.AskResult result = qaService.ask(req.getQuestion(), req.getTopK());
		List<Source> sources = result.sources().stream()
				.map(source -> new Source(source.docId(), source.title(), source.chunkNo(), source.snippet(),
						source.fileName(), source.category(), source.score()))
				.toList();
		return ApiResponse.ok(new AskResponse(result.answer(), sources, result.grounded(), result.strategy()));
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

	public record AskResponse(String answer, List<Source> sources, boolean grounded, String strategy) {
	}

	public record Source(Long docId, String title, Integer chunkNo, String snippet, String fileName, String category,
			Double score) {
	}
}
