package com.schoolmanager.backend.qa;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AcademicQueryExpanderTest {
	@Test
	void shouldExpandMajorAliasAndCurriculumIntent() {
		var variants = AcademicQueryExpander.expandQuestionVariants("计算机专业从哪里看需要修什么课？");

		assertTrue(variants.stream().anyMatch(item -> item.contains("计算机科学与技术")));
		assertTrue(variants.stream().anyMatch(item -> item.contains("培养方案")));
		assertTrue(variants.stream().anyMatch(item -> item.contains("课程设置")));
	}
}
