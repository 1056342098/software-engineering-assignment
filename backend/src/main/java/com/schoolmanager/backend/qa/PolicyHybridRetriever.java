package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.entity.PolicyDocChunk;
import com.schoolmanager.backend.policy.repo.PolicyDocChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PolicyHybridRetriever {
	private static final double K1 = 1.5;
	private static final double B = 0.75;
	private static final int PER_DOC_LIMIT = 3;
	private static final int FUSION_K = 60;

	private final PolicyDocChunkRepository chunkRepository;

	public PolicyHybridRetriever(PolicyDocChunkRepository chunkRepository) {
		this.chunkRepository = chunkRepository;
	}

	@Transactional(readOnly = true)
	public RetrievalResult retrieve(String question, int topK, PolicyQueryPlanner.QueryPlan queryPlan) {
		QueryTextAnalyzer.AnalyzedText analyzed = QueryTextAnalyzer.analyze(question);
		List<PolicyDocChunk> activeChunks = chunkRepository.findActiveChunksForRetrieval();
		if (activeChunks.isEmpty()) {
			return new RetrievalResult(List.of(), analyzed, "EMPTY_INDEX");
		}

		RetrievalComputation scoped = compute(question, activeChunks, queryPlan, true);
		List<ScoredChunk> ranked = scoped.hits();
		String strategy = scoped.strategy();
		if (ranked.isEmpty() && queryPlan != null && !queryPlan.candidateDocIds().isEmpty()) {
			RetrievalComputation fallback = compute(question, activeChunks, queryPlan, false);
			ranked = fallback.hits();
			strategy = fallback.strategy() + "_FALLBACK";
		}
		if (ranked.isEmpty()) {
			return new RetrievalResult(List.of(), analyzed, "NO_HIT");
		}
		return new RetrievalResult(diversify(ranked, topK), analyzed, strategy);
	}

	private RetrievalComputation compute(
			String question,
			List<PolicyDocChunk> activeChunks,
			PolicyQueryPlanner.QueryPlan queryPlan,
			boolean scopedSearch) {
		List<PolicyDocChunk> workingSet = activeChunks;
		if (scopedSearch && queryPlan != null && !queryPlan.candidateDocIds().isEmpty()) {
			Set<Long> allowedDocIds = new LinkedHashSet<>(queryPlan.candidateDocIds());
			workingSet = activeChunks.stream()
					.filter(chunk -> allowedDocIds.contains(chunk.getDoc().getId()))
					.toList();
		}
		if (workingSet.isEmpty()) {
			return new RetrievalComputation(List.of(), "NO_HIT");
		}

		List<String> queryVariants = buildQueryVariants(question, queryPlan);
		List<QueryHit> fulltextHits = runFullTextQueries(queryVariants, queryPlan, scopedSearch);
		Map<Long, Integer> fulltextRanks = toRankMapFromHits(fulltextHits);
		List<ScoredChunk> lexicalRanked = scoreLexically(workingSet, queryVariants, fulltextRanks);
		if (lexicalRanked.isEmpty()) {
			String strategy = fulltextHits.isEmpty() ? "HYBRID_LEXICAL" : "HYBRID_RRF";
			if (scopedSearch && queryPlan != null && !queryPlan.candidateDocIds().isEmpty()) {
				strategy = queryPlan.strategy() + "_" + strategy;
			}
			return new RetrievalComputation(List.of(), strategy);
		}

		Map<Long, Integer> lexicalRanks = toRankMapFromScoredChunks(lexicalRanked);
		List<ScoredChunk> fused = lexicalRanked.stream()
				.map(scored -> scored
						.withFusion(rrf(fulltextRanks, scored.chunkId()) + rrf(lexicalRanks, scored.chunkId())))
				.sorted(Comparator
						.comparingDouble(ScoredChunk::fusionScore)
						.thenComparingDouble(ScoredChunk::score)
						.reversed())
				.toList();

		String strategy = fulltextHits.isEmpty() ? "HYBRID_LEXICAL" : "HYBRID_RRF";
		if (scopedSearch && queryPlan != null && !queryPlan.candidateDocIds().isEmpty()) {
			strategy = queryPlan.strategy() + "_" + strategy;
		}
		return new RetrievalComputation(fused, strategy);
	}

	private static List<String> buildQueryVariants(String question, PolicyQueryPlanner.QueryPlan queryPlan) {
		LinkedHashSet<String> queries = new LinkedHashSet<>();
		if (question != null && !question.isBlank()) {
			queries.add(question.strip());
		}
		queries.addAll(AcademicQueryExpander.expandQuestionVariants(question));
		if (queryPlan != null) {
			if (queryPlan.rewrittenQuestion() != null && !queryPlan.rewrittenQuestion().isBlank()) {
				queries.add(queryPlan.rewrittenQuestion().strip());
			}
			for (String variant : queryPlan.queryVariants()) {
				if (variant != null && !variant.isBlank()) {
					queries.add(variant.strip());
				}
			}
		}
		return new ArrayList<>(queries);
	}

	private List<QueryHit> runFullTextQueries(
			List<String> queryVariants,
			PolicyQueryPlanner.QueryPlan queryPlan,
			boolean scopedSearch) {
		LinkedHashMap<Long, QueryHit> merged = new LinkedHashMap<>();
		Set<Long> allowedDocIds = scopedSearch && queryPlan != null && !queryPlan.candidateDocIds().isEmpty()
				? new LinkedHashSet<>(queryPlan.candidateDocIds())
				: Set.of();
		for (String query : queryVariants) {
			if (query == null || query.isBlank()) {
				continue;
			}
			for (PolicyDocChunkRepository.PolicyChunkSearchRow row : chunkRepository.searchTop(query)) {
				if (!allowedDocIds.isEmpty() && !allowedDocIds.contains(row.getDocId())) {
					continue;
				}
				QueryHit current = new QueryHit(row.getChunkId(), row.getScore() == null ? 0.0 : row.getScore());
				QueryHit existing = merged.get(row.getChunkId());
				if (existing == null || current.score() > existing.score()) {
					merged.put(row.getChunkId(), current);
				}
			}
		}
		return new ArrayList<>(merged.values());
	}

	private static Map<Long, Integer> toRankMapFromHits(List<QueryHit> hits) {
		Map<Long, Integer> out = new LinkedHashMap<>();
		int rank = 0;
		for (QueryHit hit : hits) {
			out.putIfAbsent(hit.chunkId(), rank++);
		}
		return out;
	}

	private static Map<Long, Integer> toRankMapFromScoredChunks(List<ScoredChunk> hits) {
		Map<Long, Integer> out = new LinkedHashMap<>();
		int rank = 0;
		for (ScoredChunk hit : hits) {
			out.putIfAbsent(hit.chunkId(), rank++);
		}
		return out;
	}

	private List<ScoredChunk> scoreLexically(
			List<PolicyDocChunk> chunks,
			List<String> queryVariants,
			Map<Long, Integer> fulltextRanks) {
		List<QueryTextAnalyzer.AnalyzedText> analyzedVariants = queryVariants.stream()
				.filter(query -> query != null && !query.isBlank())
				.map(QueryTextAnalyzer::analyze)
				.toList();
		LinkedHashSet<String> rankingTermsSet = new LinkedHashSet<>();
		LinkedHashSet<String> coverageTermsSet = new LinkedHashSet<>();
		for (QueryTextAnalyzer.AnalyzedText analyzed : analyzedVariants) {
			rankingTermsSet.addAll(analyzed.queryTermsForRanking());
			coverageTermsSet.addAll(analyzed.coverageTerms());
		}

		List<String> rankingTerms = new ArrayList<>(rankingTermsSet);
		if (rankingTerms.isEmpty()) {
			return List.of();
		}
		List<String> coverageTerms = coverageTermsSet.isEmpty() ? rankingTerms : new ArrayList<>(coverageTermsSet);

		Map<Long, SearchIndexDoc> indexed = chunks.stream()
				.collect(Collectors.toMap(
						PolicyDocChunk::getId,
						chunk -> indexChunk(chunk, coverageTerms),
						(a, b) -> a,
						LinkedHashMap::new));

		Map<String, Integer> docFreq = new HashMap<>();
		double totalLength = 0;
		for (SearchIndexDoc doc : indexed.values()) {
			totalLength += doc.searchTerms().size();
			Set<String> seen = new LinkedHashSet<>(doc.searchTerms());
			for (String term : coverageTerms) {
				if (seen.contains(term)) {
					docFreq.merge(term, 1, Integer::sum);
				}
			}
		}
		double avgDocLength = indexed.isEmpty() ? 1.0 : Math.max(1.0, totalLength / indexed.size());

		List<ScoredChunk> out = new ArrayList<>();
		for (SearchIndexDoc doc : indexed.values()) {
			double bm25 = 0;
			double metadata = 0;
			int coverage = 0;
			for (String term : coverageTerms) {
				int tf = doc.searchTermFreq().getOrDefault(term, 0);
				if (tf <= 0) {
					continue;
				}
				coverage++;
				double df = docFreq.getOrDefault(term, 0);
				double idf = Math.log(1.0 + ((indexed.size() - df + 0.5) / (df + 0.5)));
				double numerator = tf * (K1 + 1.0);
				double denominator = tf + K1 * (1.0 - B + B * (doc.searchTerms().size() / avgDocLength));
				bm25 += idf * (numerator / denominator);
			}

			metadata += containsBonus(doc.docTitle(), coverageTerms, 3.5, 9.0);
			metadata += containsBonus(doc.docCategory(), coverageTerms, 2.5, 6.0);
			metadata += containsBonus(doc.versionLabel(), coverageTerms, 2.0, 4.0);
			metadata += containsBonus(doc.summaryText(), coverageTerms, 2.8, 8.0);
			metadata += containsBonus(doc.standardAnswer(), coverageTerms, 3.2, 10.0);
			metadata += containsBonus(doc.fileName(), coverageTerms, 2.2, 5.0);

			double phraseBonus = phraseBonus(doc, analyzedVariants);
			double coverageBonus = coverageTerms.isEmpty() ? 0 : (coverage * 18.0 / coverageTerms.size());
			double fulltextBonus = rrf(fulltextRanks, doc.chunkId()) * 120.0;
			double finalScore = bm25 * 20.0 + metadata + phraseBonus + coverageBonus + fulltextBonus;
			if (finalScore < 8.0) {
				continue;
			}
			out.add(new ScoredChunk(
					doc.chunkId(),
					doc.docId(),
					doc.chunkNo(),
					doc.chunkText(),
					doc.docTitle(),
					doc.fileName(),
					doc.docCategory(),
					finalScore,
					0));
		}

		out.sort(Comparator
				.comparingDouble(ScoredChunk::score).reversed()
				.thenComparing(ScoredChunk::docId)
				.thenComparing(ScoredChunk::chunkNo));
		return out;
	}

	private static SearchIndexDoc indexChunk(PolicyDocChunk chunk, Collection<String> coverageTerms) {
		PolicyDoc doc = chunk.getDoc();
		String searchText = QueryTextAnalyzer.normalize(chunk.getSearchText());
		Map<String, Integer> searchTermFreq = termFreq(searchText, coverageTerms);
		List<String> searchTerms = tokenizeForLength(searchText);
		return new SearchIndexDoc(
				chunk.getId(),
				doc.getId(),
				chunk.getChunkNo(),
				chunk.getChunkText(),
				QueryTextAnalyzer.normalize(doc.getTitle()),
				QueryTextAnalyzer.normalize(doc.getCategory()),
				QueryTextAnalyzer.normalize(doc.getVersionLabel()),
				QueryTextAnalyzer.normalize(doc.getSummaryText()),
				QueryTextAnalyzer.normalize(doc.getStandardAnswer()),
				QueryTextAnalyzer.normalize(doc.getFileName()),
				searchTerms,
				searchTermFreq,
				searchText);
	}

	private static Map<String, Integer> termFreq(String normalizedSearchText, Collection<String> terms) {
		Map<String, Integer> out = new HashMap<>();
		for (String term : terms) {
			int count = countOccurrences(normalizedSearchText, term);
			if (count > 0) {
				out.put(term, count);
			}
		}
		return out;
	}

	private static List<String> tokenizeForLength(String normalizedSearchText) {
		if (normalizedSearchText.isBlank()) {
			return List.of();
		}
		return QueryTextAnalyzer.analyze(normalizedSearchText).rawTerms();
	}

	private static double containsBonus(String haystack, Collection<String> terms, double perTerm, double allBonus) {
		if (haystack == null || haystack.isBlank() || terms.isEmpty()) {
			return 0;
		}
		double score = 0;
		int matched = 0;
		for (String term : terms) {
			if (haystack.contains(term)) {
				score += perTerm;
				matched++;
			}
		}
		if (matched > 0 && matched == terms.size()) {
			score += allBonus;
		}
		return score;
	}

	private static double phraseBonus(SearchIndexDoc doc, List<QueryTextAnalyzer.AnalyzedText> analyzedVariants) {
		double score = 0;
		for (QueryTextAnalyzer.AnalyzedText analyzed : analyzedVariants) {
			String query = analyzed.normalized();
			if (query.isBlank()) {
				continue;
			}
			if (doc.searchText().contains(query)) {
				score += 24.0;
			}
			String compactQuery = query.replace(" ", "");
			if (compactQuery.length() >= 4 && doc.searchText().replace(" ", "").contains(compactQuery)) {
				score += 18.0;
			}
		}
		return score;
	}

	private static int countOccurrences(String text, String term) {
		if (text == null || text.isBlank() || term == null || term.isBlank()) {
			return 0;
		}
		int count = 0;
		int from = 0;
		while (from >= 0) {
			int idx = text.indexOf(term, from);
			if (idx < 0) {
				break;
			}
			count++;
			from = idx + term.length();
		}
		return count;
	}

	private static double rrf(Map<Long, Integer> rankMap, Long chunkId) {
		Integer rank = rankMap.get(chunkId);
		if (rank == null) {
			return 0;
		}
		return 1.0 / (FUSION_K + rank + 1.0);
	}

	private static List<ScoredChunk> diversify(List<ScoredChunk> ranked, int topK) {
		List<ScoredChunk> out = new ArrayList<>();
		Map<Long, Integer> perDoc = new HashMap<>();
		for (ScoredChunk chunk : ranked) {
			int used = perDoc.getOrDefault(chunk.docId(), 0);
			if (used >= PER_DOC_LIMIT) {
				continue;
			}
			out.add(chunk);
			perDoc.put(chunk.docId(), used + 1);
			if (out.size() >= Math.max(topK, 6)) {
				break;
			}
		}
		return out;
	}

	public record RetrievalResult(List<ScoredChunk> hits, QueryTextAnalyzer.AnalyzedText analyzedQuery,
			String strategy) {
	}

	public record ScoredChunk(Long chunkId, Long docId, Integer chunkNo, String chunkText, String title,
			String fileName,
			String category, double score, double fusionScore) {
		ScoredChunk withFusion(double fusionScore) {
			return new ScoredChunk(chunkId, docId, chunkNo, chunkText, title, fileName, category, score, fusionScore);
		}
	}

	private record QueryHit(Long chunkId, double score) {
	}

	private record SearchIndexDoc(Long chunkId, Long docId, Integer chunkNo, String chunkText, String docTitle,
			String docCategory, String versionLabel, String summaryText, String standardAnswer, String fileName,
			List<String> searchTerms, Map<String, Integer> searchTermFreq, String searchText) {
	}

	private record RetrievalComputation(List<ScoredChunk> hits, String strategy) {
	}
}
