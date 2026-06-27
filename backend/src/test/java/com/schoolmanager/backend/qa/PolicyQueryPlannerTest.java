package com.schoolmanager.backend.qa;

import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.repo.PolicyDocRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PolicyQueryPlannerTest {
	@Mock
	private PolicyDocRepository docRepository;

	@Mock
	private LlmClient llmClient;

	@InjectMocks
	private PolicyQueryPlanner planner;

	@Test
	void shouldLetLlmRouteNaturalQuestionToCultivationPlan() throws Exception {
		PolicyDoc cultivationPlan = doc(101L, "计算机科学与技术专业培养方案", "培养方案", "列出专业课程、学分和修读要求。");
		PolicyDoc scholarship = doc(102L, "奖学金评审办法", "奖助", "说明奖学金申请条件和评审流程。");

		when(docRepository.findByStatusOrderByIdDesc("ACTIVE")).thenReturn(List.of(cultivationPlan, scholarship));
		when(llmClient.planRetrieval(anyString(), anyString())).thenReturn(Optional.of(
				new LlmClient.RetrievalPlan(
						"应该查看培养方案中的课程设置与修读要求",
						List.of("培养方案 课程设置 修读要求", "计算机专业 修什么课"),
						List.of(101L),
						"用户想知道应依据哪份文件查看修读课程，最可能是培养方案。")));

		PolicyQueryPlanner.QueryPlan plan = planner.plan("从哪里看需要修什么课？");

		assertEquals("LLM_ROUTER", plan.strategy());
		assertFalse(plan.candidateDocIds().isEmpty());
		assertEquals(101L, plan.candidateDocIds().get(0));
		assertFalse(plan.queryVariants().stream().noneMatch(query -> query.contains("培养方案")));
	}

	private static PolicyDoc doc(Long id, String title, String category, String summaryText) throws Exception {
		PolicyDoc doc = new PolicyDoc();
		setField(doc, "id", id);
		doc.setTitle(title);
		doc.setCategory(category);
		doc.setSummaryText(summaryText);
		doc.setFileName(title + ".pdf");
		doc.setStatus("ACTIVE");
		return doc;
	}

	private static void setField(Object target, String fieldName, Object value) throws Exception {
		Field field = target.getClass().getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(target, value);
	}
}
