package com.schoolmanager.backend.qa;

import java.util.List;
import java.util.Optional;

public interface LlmClient {
	Optional<String> answer(String question, String groundedContext);

	default Optional<RetrievalPlan> planRetrieval(String question, String documentCatalog) {
		return Optional.empty();
	}

	record RetrievalPlan(String rewrittenQuestion, List<String> queryVariants, List<Long> candidateDocIds,
			String rationale) {
	}
}
