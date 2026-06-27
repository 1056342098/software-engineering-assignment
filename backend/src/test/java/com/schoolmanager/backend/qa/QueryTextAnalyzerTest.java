package com.schoolmanager.backend.qa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryTextAnalyzerTest {
	@Test
	void shouldExtractChineseDomainTermsFromNaturalQuestion() {
		QueryTextAnalyzer.AnalyzedText analyzed = QueryTextAnalyzer.analyze("培养方案里计算机专业需要学什么课程？");

		assertFalse(analyzed.queryTermsForRanking().isEmpty());
		assertTrue(analyzed.queryTermsForRanking().stream().anyMatch(term -> term.contains("计算机")));
		assertTrue(analyzed.queryTermsForRanking().stream().anyMatch(term -> term.contains("课程")));
	}
}
