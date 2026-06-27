package com.schoolmanager.backend.qa;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class QueryTextAnalyzer {
	private static final Pattern ALNUM = Pattern.compile("[a-z0-9]{2,}");
	private static final Set<String> STOP_WORDS = Set.of(
			"什么", "哪些", "多少", "怎么", "如何", "需要", "请问", "一下", "里面", "关于", "有关", "相关",
			"是否", "可以", "进行", "培养", "方案", "文件", "规定", "政策", "要求", "这个", "那个",
			"学生", "老师", "学院", "学校", "我们", "你们", "他们", "以及", "或者", "还有", "具体");

	private QueryTextAnalyzer() {
	}

	public static AnalyzedText analyze(String text) {
		String normalized = normalize(text);
		List<String> rawTerms = extractTerms(normalized);
		List<String> importantTerms = rawTerms.stream()
				.filter(term -> !STOP_WORDS.contains(term))
				.toList();
		List<String> finalTerms = importantTerms.isEmpty() ? rawTerms : importantTerms;
		String keywordQuery = String.join(" ", finalTerms);
		return new AnalyzedText(text == null ? "" : text.strip(), normalized, rawTerms, finalTerms, keywordQuery);
	}

	public static String normalize(String text) {
		if (text == null) {
			return "";
		}
		String normalized = Normalizer.normalize(text, Normalizer.Form.NFKC)
				.toLowerCase(Locale.ROOT)
				.replace('\u3000', ' ')
				.replace('\r', ' ')
				.replace('\n', ' ')
				.replaceAll("[\\p{Punct}，。！？；：、“”‘’（）《》【】…·]+", " ")
				.replaceAll("\\s+", " ")
				.strip();
		return normalized;
	}

	private static List<String> extractTerms(String normalized) {
		if (normalized.isBlank()) {
			return List.of();
		}
		LinkedHashSet<String> terms = new LinkedHashSet<>();
		StringBuilder cjk = new StringBuilder();
		StringBuilder latin = new StringBuilder();
		for (int i = 0; i < normalized.length(); i++) {
			char ch = normalized.charAt(i);
			if (isCjk(ch)) {
				flushLatin(latin, terms);
				cjk.append(ch);
				continue;
			}
			flushCjk(cjk, terms);
			if (Character.isLetterOrDigit(ch)) {
				latin.append(ch);
			} else {
				flushLatin(latin, terms);
			}
		}
		flushCjk(cjk, terms);
		flushLatin(latin, terms);
		if (terms.isEmpty() && !normalized.isBlank()) {
			terms.add(normalized);
		}
		return List.copyOf(terms);
	}

	private static void flushCjk(StringBuilder buffer, Set<String> out) {
		if (buffer.length() == 0) {
			return;
		}
		String block = buffer.toString();
		if (block.length() >= 2) {
			out.add(block);
		}
		for (int i = 0; i < block.length(); i++) {
			int biEnd = Math.min(i + 2, block.length());
			if (biEnd - i >= 2) {
				out.add(block.substring(i, biEnd));
			}
			int triEnd = Math.min(i + 3, block.length());
			if (triEnd - i >= 3) {
				out.add(block.substring(i, triEnd));
			}
		}
		buffer.setLength(0);
	}

	private static void flushLatin(StringBuilder buffer, Set<String> out) {
		if (buffer.length() == 0) {
			return;
		}
		String word = buffer.toString();
		if (ALNUM.matcher(word).matches()) {
			out.add(word);
		}
		buffer.setLength(0);
	}

	private static boolean isCjk(char ch) {
		return Character.UnicodeScript.of(ch) == Character.UnicodeScript.HAN;
	}

	record AnalyzedText(String raw, String normalized, List<String> rawTerms, List<String> importantTerms,
			String keywordQuery) {
		List<String> queryTermsForRanking() {
			return importantTerms.isEmpty() ? rawTerms : importantTerms;
		}

		List<String> coverageTerms() {
			if (importantTerms.isEmpty()) {
				return rawTerms.size() <= 6 ? rawTerms : new ArrayList<>(rawTerms.subList(0, 6));
			}
			return importantTerms.size() <= 8 ? importantTerms : new ArrayList<>(importantTerms.subList(0, 8));
		}
	}
}
