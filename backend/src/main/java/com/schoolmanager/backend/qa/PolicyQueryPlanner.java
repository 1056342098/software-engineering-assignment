package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.repo.PolicyDocRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyQueryPlanner {
	private static final int MAX_DOCS_IN_CATALOG = 24;
	private static final int MAX_SCOPED_DOCS = 6;

	private final PolicyDocRepository docRepository;
	private final LlmClient llmClient;

	public PolicyQueryPlanner(PolicyDocRepository docRepository, LlmClient llmClient) {
		this.docRepository = docRepository;
		this.llmClient = llmClient;
	}

	public QueryPlan plan(String question) {
		List<PolicyDoc> activeDocs = docRepository.findByStatusOrderByIdDesc("ACTIVE");
		if (activeDocs.isEmpty()) {
			return new QueryPlan(question.strip(), List.of(question.strip()), List.of(), "EMPTY_CATALOG", "没有可用政策文件");
		}

		List<DocCandidate> heuristic = heuristicRank(question, activeDocs);
		String catalog = buildCatalog(activeDocs, heuristic);
		Map<Long, PolicyDoc> docsById = new LinkedHashMap<>();
		for (PolicyDoc doc : activeDocs) {
			docsById.put(doc.getId(), doc);
		}

		LinkedHashSet<Long> selectedDocIds = new LinkedHashSet<>();
		LinkedHashSet<String> queryVariants = new LinkedHashSet<>();
		queryVariants.add(question.strip());
		queryVariants.addAll(AcademicQueryExpander.expandQuestionVariants(question));
		String rewrittenQuestion = question.strip();
		String rationale = "使用本地规则进行查询规划";
		String strategy = "HEURISTIC_ROUTER";

		OptionalPlan llmPlan = OptionalPlan.from(llmClient.planRetrieval(question, catalog).orElse(null));
		if (llmPlan.available()) {
			strategy = "LLM_ROUTER";
			if (!llmPlan.rewrittenQuestion().isBlank()) {
				rewrittenQuestion = llmPlan.rewrittenQuestion();
			}
			for (Long docId : llmPlan.candidateDocIds()) {
				if (docsById.containsKey(docId)) {
					selectedDocIds.add(docId);
				}
			}
			for (String variant : llmPlan.queryVariants()) {
				if (!variant.isBlank()) {
					queryVariants.add(variant);
				}
			}
			if (!llmPlan.rationale().isBlank()) {
				rationale = llmPlan.rationale();
			}
		}

		for (DocCandidate candidate : heuristic) {
			if (candidate.score() <= 0) {
				continue;
			}
			if (selectedDocIds.size() >= MAX_SCOPED_DOCS) {
				break;
			}
			selectedDocIds.add(candidate.doc().getId());
		}

		QueryTextAnalyzer.AnalyzedText analyzed = QueryTextAnalyzer.analyze(question);
		for (String term : analyzed.coverageTerms()) {
			queryVariants.add(term);
		}
		if (!queryVariants.contains(rewrittenQuestion)) {
			queryVariants.add(rewrittenQuestion);
		}

		return new QueryPlan(
				rewrittenQuestion,
				queryVariants.stream().filter(value -> value != null && !value.isBlank()).distinct().toList(),
				new ArrayList<>(selectedDocIds),
				strategy,
				rationale);
	}

	private static String buildCatalog(List<PolicyDoc> activeDocs, List<DocCandidate> heuristic) {
		Map<Long, Double> heuristicScores = new LinkedHashMap<>();
		for (DocCandidate candidate : heuristic) {
			heuristicScores.put(candidate.doc().getId(), candidate.score());
		}
		StringBuilder sb = new StringBuilder();
		int count = 0;
		for (PolicyDoc doc : activeDocs) {
			if (count >= MAX_DOCS_IN_CATALOG) {
				break;
			}
			sb.append("docId=").append(doc.getId())
					.append(" | 标题=").append(nullToEmpty(doc.getTitle()))
					.append(" | 分类=").append(nullToEmpty(doc.getCategory()))
					.append(" | 版本=").append(nullToEmpty(doc.getVersionLabel()))
					.append(" | 文件名=").append(nullToEmpty(doc.getFileName()));
			if (doc.getSummaryText() != null && !doc.getSummaryText().isBlank()) {
				sb.append(" | 摘要=").append(trim(doc.getSummaryText(), 160));
			}
			if (doc.getStandardAnswer() != null && !doc.getStandardAnswer().isBlank()) {
				sb.append(" | 标准答案=").append(trim(doc.getStandardAnswer(), 120));
			}
			Double heuristicScore = heuristicScores.get(doc.getId());
			if (heuristicScore != null && heuristicScore > 0) {
				sb.append(" | 规则相关度=").append(String.format(java.util.Locale.ROOT, "%.2f", heuristicScore));
			}
			sb.append("\n");
			count++;
		}
		return sb.toString().strip();
	}

	private static List<DocCandidate> heuristicRank(String question, List<PolicyDoc> docs) {
		LinkedHashSet<String> termSet = new LinkedHashSet<>();
		QueryTextAnalyzer.AnalyzedText analyzed = QueryTextAnalyzer.analyze(question);
		termSet.addAll(analyzed.coverageTerms());
		for (String variant : AcademicQueryExpander.expandQuestionVariants(question)) {
			termSet.addAll(QueryTextAnalyzer.analyze(variant).coverageTerms());
		}
		List<String> terms = new ArrayList<>(termSet);
		List<DocCandidate> ranked = new ArrayList<>();
		for (PolicyDoc doc : docs) {
			String title = QueryTextAnalyzer.normalize(doc.getTitle());
			String category = QueryTextAnalyzer.normalize(doc.getCategory());
			String summary = QueryTextAnalyzer.normalize(doc.getSummaryText());
			String standardAnswer = QueryTextAnalyzer.normalize(doc.getStandardAnswer());
			String fileName = QueryTextAnalyzer.normalize(doc.getFileName());
			double score = containsBonus(title, terms, 3.6, 10)
					+ containsBonus(category, terms, 2.0, 4)
					+ containsBonus(summary, terms, 2.8, 8)
					+ containsBonus(standardAnswer, terms, 2.2, 6)
					+ containsBonus(fileName, terms, 1.5, 3);
			if (isCurriculumIntent(question) && containsCultivationPlanSignal(doc)) {
				score += 8.0;
			}
			ranked.add(new DocCandidate(doc, score));
		}
		ranked.sort(Comparator.comparingDouble(DocCandidate::score).reversed()
				.thenComparing(candidate -> candidate.doc().getId()));
		return ranked;
	}

	private static double containsBonus(String haystack, List<String> terms, double perTerm, double allBonus) {
		if (haystack == null || haystack.isBlank() || terms.isEmpty()) {
			return 0;
		}
		Set<String> matched = new LinkedHashSet<>();
		for (String term : terms) {
			if (haystack.contains(term)) {
				matched.add(term);
			}
		}
		if (matched.isEmpty()) {
			return 0;
		}
		double score = matched.size() * perTerm;
		if (matched.size() == terms.size()) {
			score += allBonus;
		}
		return score;
	}

	private static boolean isCurriculumIntent(String question) {
		String normalized = QueryTextAnalyzer.normalize(question);
		return normalized.contains("修什么课")
				|| normalized.contains("学什么课")
				|| normalized.contains("课程")
				|| normalized.contains("培养方案")
				|| normalized.contains("修读要求");
	}

	private static boolean containsCultivationPlanSignal(PolicyDoc doc) {
		String joined = QueryTextAnalyzer.normalize(
				nullToEmpty(doc.getTitle()) + "\n" + nullToEmpty(doc.getCategory()) + "\n"
						+ nullToEmpty(doc.getSummaryText()) + "\n" + nullToEmpty(doc.getFileName()));
		return joined.contains("培养方案")
				|| joined.contains("课程设置")
				|| joined.contains("修读要求")
				|| joined.contains("教学计划");
	}

	private static String trim(String value, int limit) {
		String normalized = value.replace("\r", "").replaceAll("\\s+", " ").strip();
		return normalized.length() <= limit ? normalized : normalized.substring(0, limit) + "...";
	}

	private static String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	public record QueryPlan(String rewrittenQuestion, List<String> queryVariants, List<Long> candidateDocIds,
			String strategy,
			String rationale) {
	}

	private record DocCandidate(PolicyDoc doc, double score) {
	}

	private record OptionalPlan(boolean available, String rewrittenQuestion, List<String> queryVariants,
			List<Long> candidateDocIds, String rationale) {
		static OptionalPlan from(LlmClient.RetrievalPlan plan) {
			if (plan == null) {
				return new OptionalPlan(false, "", List.of(), List.of(), "");
			}
			return new OptionalPlan(
					true,
					plan.rewrittenQuestion() == null ? "" : plan.rewrittenQuestion().strip(),
					plan.queryVariants() == null ? List.of()
							: plan.queryVariants().stream()
									.filter(value -> value != null && !value.isBlank())
									.map(String::strip)
									.toList(),
					plan.candidateDocIds() == null ? List.of()
							: plan.candidateDocIds().stream()
									.filter(id -> id != null && id > 0)
									.toList(),
					plan.rationale() == null ? "" : plan.rationale().strip());
		}
	}
}
