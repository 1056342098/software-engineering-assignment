package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.entity.PolicyDocChunk;
import com.schoolmanager.backend.policy.repo.PolicyDocChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyHybridRetrieverTest {
	@Mock
	private PolicyDocChunkRepository chunkRepository;

	@InjectMocks
	private PolicyHybridRetriever retriever;

	@Test
	void shouldRetrieveCourseChunkForRoutedNaturalQuestion() throws Exception {
		PolicyDocChunk courseChunk = chunk(
				11L,
				101L,
				0,
				"计算机科学与技术专业学生需完成程序设计、数据结构、计算机组成原理、操作系统、数据库系统、计算机网络等核心课程。",
				"计算机科学与技术专业培养方案",
				"培养方案");
		PolicyDocChunk unrelatedChunk = chunk(
				12L,
				102L,
				0,
				"奖学金评审主要依据学业成绩、综合表现和日常行为规范。",
				"奖学金评审办法",
				"奖助");

		when(chunkRepository.findActiveChunksForRetrieval()).thenReturn(List.of(courseChunk, unrelatedChunk));
		when(chunkRepository.searchTop(anyString())).thenReturn(List.of());

		PolicyQueryPlanner.QueryPlan queryPlan = new PolicyQueryPlanner.QueryPlan(
				"应该查看培养方案中的课程设置与修读要求",
				List.of("培养方案 课程设置 修读要求", "计算机专业 需要学什么课程"),
				List.of(101L),
				"LLM_ROUTER",
				"用户在询问查看课程依据，应优先查培养方案");

		PolicyHybridRetriever.RetrievalResult result = retriever.retrieve("从哪里看需要修什么课？", 5, queryPlan);

		assertFalse(result.hits().isEmpty());
		assertEquals(101L, result.hits().get(0).docId());
		assertEquals("计算机科学与技术专业培养方案", result.hits().get(0).title());
	}

	private static PolicyDocChunk chunk(Long chunkId, Long docId, int chunkNo, String chunkText, String title, String category)
			throws Exception {
		PolicyDoc doc = new PolicyDoc();
		setField(doc, "id", docId);
		doc.setTitle(title);
		doc.setCategory(category);
		doc.setFileName(title + ".pdf");
		doc.setSummaryText("本培养方案列出专业课程、学分和修读要求。");
		doc.setStandardAnswer("专业课程以培养方案为准。");
		doc.setStatus("ACTIVE");

		PolicyDocChunk chunk = new PolicyDocChunk();
		setField(chunk, "id", chunkId);
		chunk.setDoc(doc);
		chunk.setChunkNo(chunkNo);
		chunk.setChunkText(chunkText);
		chunk.setSearchText(String.join("\n", title, category, doc.getSummaryText(), doc.getStandardAnswer(), chunkText));
		return chunk;
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
