package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.repo.PolicyDocRepository;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class QaService {
	private final PolicyDocRepository docRepository;
	private final PolicyQueryPlanner queryPlanner;
	private final PolicyHybridRetriever hybridRetriever;
	private final LlmClient llmClient;

	public QaService(
			PolicyDocRepository docRepository,
			PolicyQueryPlanner queryPlanner,
			PolicyHybridRetriever hybridRetriever,
			LlmClient llmClient) {
		this.docRepository = docRepository;
		this.queryPlanner = queryPlanner;
		this.hybridRetriever = hybridRetriever;
		this.llmClient = llmClient;
	}

	public AskResult ask(String question, Integer topK) {
		int limit = Math.min(Math.max(topK == null ? 5 : topK, 1), 8);
		PolicyQueryPlanner.QueryPlan queryPlan = queryPlanner.plan(question);
		PolicyHybridRetriever.RetrievalResult retrieval = hybridRetriever.retrieve(question, limit, queryPlan);
		List<PolicyHybridRetriever.ScoredChunk> hits = retrieval.hits().stream()
				.limit(limit)
				.toList();
		if (hits.isEmpty()) {
			return new AskResult(
					"未找到相关政策资料。请尝试补充更明确的关键词，或联系老师确认最新要求。",
					List.of(),
					false,
					retrieval.strategy());
		}

		Map<Long, PolicyDoc> docs = new LinkedHashMap<>();
		for (PolicyHybridRetriever.ScoredChunk hit : hits) {
			docRepository.findById(hit.docId()).ifPresent(doc -> docs.putIfAbsent(doc.getId(), doc));
		}

		List<AnswerSource> sources = hits.stream()
				.map(hit -> toSource(hit, docs.get(hit.docId())))
				.filter(source -> source.docId() != null)
				.toList();

		String groundedContext = buildGroundedContext(question, queryPlan, hits, docs);
		Optional<String> llmAnswer = llmClient.answer(question, groundedContext);
		if (llmAnswer.isPresent()) {
			return new AskResult(llmAnswer.get(), sources, true, retrieval.strategy() + "_LLM");
		}
		return new AskResult(buildFallbackAnswer(question, hits, docs), sources, true, retrieval.strategy() + "_ONLY");
	}

	private static AnswerSource toSource(PolicyHybridRetriever.ScoredChunk hit, PolicyDoc doc) {
		if (doc == null) {
			return new AnswerSource(null, null, hit.chunkNo(), trimSnippet(hit.chunkText()), null, null, hit.score());
		}
		return new AnswerSource(
				doc.getId(),
				doc.getTitle(),
				hit.chunkNo(),
				trimSnippet(hit.chunkText()),
				doc.getFileName(),
				doc.getCategory(),
				hit.score());
	}

	private static String buildGroundedContext(
			String question,
			PolicyQueryPlanner.QueryPlan queryPlan,
			List<PolicyHybridRetriever.ScoredChunk> hits,
			Map<Long, PolicyDoc> docs) {
		StringBuilder sb = new StringBuilder();
		sb.append("问题：").append(question.strip()).append("\n\n");
		if (queryPlan != null) {
			sb.append("查询改写：").append(nullToEmpty(queryPlan.rewrittenQuestion())).append("\n");
			if (!queryPlan.queryVariants().isEmpty()) {
				sb.append("扩展查询：").append(String.join(" | ", queryPlan.queryVariants())).append("\n");
			}
			if (!isBlank(queryPlan.rationale())) {
				sb.append("文件路由依据：").append(queryPlan.rationale()).append("\n");
			}
			sb.append("\n");
		}
		int idx = 1;
		for (PolicyHybridRetriever.ScoredChunk hit : hits) {
			PolicyDoc doc = docs.get(hit.docId());
			if (doc == null) {
				continue;
			}
			sb.append("资料 ").append(idx++).append("\n");
			sb.append("标题：").append(nullToEmpty(doc.getTitle())).append("\n");
			sb.append("分类：").append(nullToEmpty(doc.getCategory())).append("\n");
			sb.append("版本：").append(nullToEmpty(doc.getVersionLabel())).append("\n");
			if (!isBlank(doc.getStandardAnswer())) {
				sb.append("标准答案：").append(doc.getStandardAnswer().strip()).append("\n");
			}
			if (!isBlank(doc.getSummaryText())) {
				sb.append("摘要：").append(doc.getSummaryText().strip()).append("\n");
			}
			sb.append("命中原文片段：").append(trimSnippet(hit.chunkText())).append("\n");
			sb.append("片段编号：").append(hit.chunkNo()).append("\n");
			sb.append("检索分数：").append(String.format(java.util.Locale.ROOT, "%.3f", hit.score())).append("\n\n");
		}
		return sb.toString().strip();
	}

	private static String buildFallbackAnswer(
			String question,
			List<PolicyHybridRetriever.ScoredChunk> hits,
			Map<Long, PolicyDoc> docs) {
		PolicyHybridRetriever.ScoredChunk best = hits.get(0);
		PolicyDoc bestDoc = docs.get(best.docId());
		StringBuilder sb = new StringBuilder();
		if (bestDoc != null && !isBlank(bestDoc.getStandardAnswer())) {
			sb.append(bestDoc.getStandardAnswer().strip()).append("\n\n");
		} else if (bestDoc != null && !isBlank(bestDoc.getSummaryText())) {
			sb.append(bestDoc.getSummaryText().strip()).append("\n\n");
		} else {
			sb.append("根据当前命中的政策资料，整理到以下关键信息：\n\n");
		}
		int shown = 0;
		for (PolicyHybridRetriever.ScoredChunk hit : hits) {
			PolicyDoc doc = docs.get(hit.docId());
			if (doc == null) {
				continue;
			}
			sb.append(shown + 1)
					.append(". ")
					.append(doc.getTitle())
					.append("：")
					.append(trimSnippet(hit.chunkText()))
					.append("\n");
			shown++;
			if (shown >= 3) {
				break;
			}
		}
		sb.append("\n当前回答基于检索结果整理，若涉及具体时限、材料清单或资格条件，请以原文为准。");
		return sb.toString().strip();
	}

	private static String trimSnippet(String value) {
		if (value == null) {
			return "";
		}
		String normalized = value.replace("\r", "").replaceAll("\\s+", " ").strip();
		if (normalized.length() <= 220) {
			return normalized;
		}
		return normalized.substring(0, 220) + "...";
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public record AskResult(String answer, List<AnswerSource> sources, boolean grounded, String strategy) {
	}

	public record AnswerSource(Long docId, String title, Integer chunkNo, String snippet, String fileName,
			String category, Double score) {
	}
}
