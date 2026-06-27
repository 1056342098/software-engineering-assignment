package com.schoolmanager.backend.qa;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class AcademicQueryExpander {
	private static final Map<String, List<String>> MAJOR_ALIASES = Map.ofEntries(
			Map.entry("计算机科学与技术", List.of("计算机专业", "计科", "cs", "计算机科学与技术专业")),
			Map.entry("软件工程", List.of("软件专业", "软工", "软件工程专业")),
			Map.entry("网络工程", List.of("网络专业", "网络工程专业")),
			Map.entry("数据科学与大数据技术", List.of("大数据专业", "数据科学专业", "大数据")),
			Map.entry("人工智能", List.of("ai专业", "人工智能专业", "智科")));

	private static final List<String> CURRICULUM_INTENTS = List.of(
			"修什么课", "学什么课", "哪些课程", "课程", "必修课", "选修课", "培养方案", "课程设置", "修读要求", "教学计划");

	private static final List<String> FILE_INTENTS = List.of(
			"从哪里看", "看什么文件", "依据什么", "查什么文件", "去哪看", "在哪看");

	private AcademicQueryExpander() {
	}

	public static List<String> expandQuestionVariants(String question) {
		LinkedHashSet<String> variants = new LinkedHashSet<>();
		if (question == null || question.isBlank()) {
			return List.of();
		}
		String trimmed = question.strip();
		variants.add(trimmed);

		String normalized = QueryTextAnalyzer.normalize(trimmed);
		for (Map.Entry<String, List<String>> entry : MAJOR_ALIASES.entrySet()) {
			String formal = entry.getKey();
			for (String alias : entry.getValue()) {
				String aliasNormalized = QueryTextAnalyzer.normalize(alias);
				if (normalized.contains(aliasNormalized)) {
					variants.add(replaceIgnoreCase(trimmed, alias, formal));
					variants.add(formal + "专业 课程设置");
					variants.add(formal + "专业 修读要求");
					variants.add(formal + "培养方案");
				}
				if (normalized.contains(QueryTextAnalyzer.normalize(formal))) {
					variants.add(formal + "专业 课程设置");
					for (String aliasVariant : entry.getValue()) {
						variants.add(aliasVariant + " 课程设置");
					}
				}
			}
		}

		if (containsAny(normalized, CURRICULUM_INTENTS)) {
			variants.add(trimmed + " 培养方案");
			variants.add(trimmed + " 课程设置");
			variants.add(trimmed + " 修读要求");
		}
		if (containsAny(normalized, FILE_INTENTS)) {
			variants.add("应该查看什么文件");
			variants.add("依据什么文件查看课程设置");
			variants.add("培养方案 课程设置 修读要求");
		}

		return variants.stream()
				.map(String::strip)
				.filter(value -> !value.isBlank())
				.toList();
	}

	public static String buildDocumentHints(String... texts) {
		LinkedHashSet<String> hints = new LinkedHashSet<>();
		String joined = String.join("\n", texts).toLowerCase(Locale.ROOT);
		String normalized = QueryTextAnalyzer.normalize(joined);

		if (normalized.contains("培养方案")) {
			hints.add("培养方案");
			hints.add("课程设置");
			hints.add("修读要求");
			hints.add("教学计划");
			hints.add("课程体系");
			hints.add("必修课");
			hints.add("选修课");
			hints.add("学分要求");
		}
		for (Map.Entry<String, List<String>> entry : MAJOR_ALIASES.entrySet()) {
			String formal = entry.getKey();
			if (normalized.contains(QueryTextAnalyzer.normalize(formal))) {
				hints.add(formal);
				hints.add(formal + "专业");
				hints.addAll(entry.getValue());
			}
			for (String alias : entry.getValue()) {
				if (normalized.contains(QueryTextAnalyzer.normalize(alias))) {
					hints.add(formal);
					hints.add(formal + "专业");
					hints.add(alias);
				}
			}
		}
		return String.join("\n", hints);
	}

	private static boolean containsAny(String normalized, List<String> samples) {
		for (String sample : samples) {
			if (normalized.contains(QueryTextAnalyzer.normalize(sample))) {
				return true;
			}
		}
		return false;
	}

	private static String replaceIgnoreCase(String source, String target, String replacement) {
		String normalizedSource = source.toLowerCase(Locale.ROOT);
		String normalizedTarget = target.toLowerCase(Locale.ROOT);
		int index = normalizedSource.indexOf(normalizedTarget);
		if (index < 0) {
			return source;
		}
		return source.substring(0, index) + replacement + source.substring(index + target.length());
	}
}
