package com.schoolmanager.backend.qa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.config.AppProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
	private final AppProperties.Llm properties;
	private final ObjectMapper objectMapper;

	public OpenAiCompatibleLlmClient(AppProperties appProperties, ObjectMapper objectMapper) {
		this.properties = appProperties.getLlm();
		this.objectMapper = objectMapper;
	}

	@Override
	public Optional<String> answer(String question, String groundedContext) {
		List<Map<String, Object>> messages = List.of(
				Map.of(
						"role", "system",
						"content",
						"""
								你是学院服务平台的政策问答助手。
								你只能依据提供的政策资料回答，不得编造流程、时间、材料、要求或结论。
								若资料不足，请明确回答“当前资料不足以支持确定结论”，并建议联系老师或查看原文。
								优先使用标准答案，其次使用摘要，再结合原文片段。
								若多个资料存在差异，要明确说明“请以版本更新、更具体或教师确认的原文为准”。
								回答要简洁、结构化，优先给出结论，再列关键依据，避免输出与资料无关的内容。
								""".strip()),
				Map.of(
						"role", "user",
						"content", "学生问题：\n" + question + "\n\n可用资料：\n" + groundedContext));
		return complete(messages, 0.2);
	}

	@Override
	public Optional<RetrievalPlan> planRetrieval(String question, String documentCatalog) {
		List<Map<String, Object>> messages = List.of(
				Map.of(
						"role", "system",
						"content",
						"""
								你是学院服务平台的政策文件路由器。
								你的任务不是回答问题，而是根据“用户问题”和“当前文件目录”，判断最应该查阅哪些文件，并把口语化问题改写成更适合检索的查询。
								必须只从提供的文件目录中选择候选文件，candidateDocIds 只能填写目录中已有的 docId。
								如果问题是在问“去哪里看”“看什么文件”“应该依据什么文件”，要优先选择最可能承载正式要求的政策文件，例如培养方案、学籍规定、奖学金办法、审批流程等。
								输出必须是 JSON，不要输出 markdown，不要解释。
								JSON 结构：
								{
								  "rewrittenQuestion": "改写后的检索问题",
								  "queryVariants": ["扩展查询1", "扩展查询2"],
								  "candidateDocIds": [1, 2],
								  "rationale": "为什么优先看这些文件"
								}
								queryVariants 最多 4 条，candidateDocIds 最多 5 个。
								""".strip()),
				Map.of(
						"role", "user",
						"content", "用户问题：\n" + question + "\n\n当前文件目录：\n" + documentCatalog));
		Optional<String> content = complete(messages, 0.1);
		if (content.isEmpty()) {
			return Optional.empty();
		}
		return parseRetrievalPlan(content.get());
	}

	private Optional<String> complete(List<Map<String, Object>> messages, double temperature) {
		if (!properties.isEnabled()) {
			return Optional.empty();
		}
		if (isBlank(properties.getBaseUrl()) || isBlank(properties.getApiKey()) || isBlank(properties.getModel())) {
			return Optional.empty();
		}
		try {
			RestClient client = RestClient.builder()
					.baseUrl(properties.getBaseUrl())
					.requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {
						{
							setConnectTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
							setReadTimeout(Duration.ofSeconds(properties.getTimeoutSeconds()));
						}
					})
					.build();

			Map<String, Object> payload = Map.of(
					"model", properties.getModel(),
					"temperature", temperature,
					"messages", messages);

			String body = client.post()
					.uri(properties.getEndpoint())
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.accept(MediaType.APPLICATION_JSON)
					.body(payload)
					.retrieve()
					.body(String.class);

			if (isBlank(body)) {
				return Optional.empty();
			}
			JsonNode root = objectMapper.readTree(body);
			JsonNode choices = root.path("choices");
			if (!choices.isArray() || choices.isEmpty()) {
				return Optional.empty();
			}
			String content = choices.get(0).path("message").path("content").asText(null);
			if (isBlank(content)) {
				return Optional.empty();
			}
			return Optional.of(content.strip());
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	private Optional<RetrievalPlan> parseRetrievalPlan(String raw) {
		try {
			String candidate = stripCodeFence(raw);
			JsonNode root = objectMapper.readTree(candidate);
			String rewrittenQuestion = root.path("rewrittenQuestion").asText("");
			String rationale = root.path("rationale").asText("");
			List<String> queryVariants = new ArrayList<>();
			JsonNode variantNode = root.path("queryVariants");
			if (variantNode.isArray()) {
				for (JsonNode item : variantNode) {
					String value = item.asText("");
					if (!isBlank(value)) {
						queryVariants.add(value.strip());
					}
				}
			}
			List<Long> candidateDocIds = new ArrayList<>();
			JsonNode docNode = root.path("candidateDocIds");
			if (docNode.isArray()) {
				for (JsonNode item : docNode) {
					if (item.canConvertToLong()) {
						candidateDocIds.add(item.asLong());
					}
				}
			}
			return Optional.of(new RetrievalPlan(
					isBlank(rewrittenQuestion) ? "" : rewrittenQuestion.strip(),
					queryVariants,
					candidateDocIds,
					isBlank(rationale) ? "" : rationale.strip()));
		} catch (Exception ignored) {
			return Optional.empty();
		}
	}

	private static String stripCodeFence(String raw) {
		String value = raw.strip();
		if (value.startsWith("```")) {
			int firstNewline = value.indexOf('\n');
			if (firstNewline >= 0) {
				value = value.substring(firstNewline + 1);
			}
			if (value.endsWith("```")) {
				value = value.substring(0, value.length() - 3);
			}
		}
		return value.strip();
	}

	private static boolean isBlank(String value) {
		return value == null || value.isBlank();
	}
}
