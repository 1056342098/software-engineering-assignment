package com.schoolmanager.backend.qa;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.qa.entity.QaQuestion;
import com.schoolmanager.backend.qa.repo.QaQuestionRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/selftest")
public class SelfTestController {

    private final QaQuestionRepository qaQuestionRepository;
    private final ObjectMapper objectMapper;

    public SelfTestController(QaQuestionRepository qaQuestionRepository, ObjectMapper objectMapper) {
        this.qaQuestionRepository = qaQuestionRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/questions/{type}")
    public ApiResponse<List<QuestionDto>> getQuestions(@PathVariable String type) {
        List<QaQuestion> questions = qaQuestionRepository.findByType(type.toUpperCase());
        List<QuestionDto> dtos = questions.stream().map(q -> {
            try {
                List<String> options = objectMapper.readValue(q.getOptionsJson(), new TypeReference<List<String>>() {});
                return new QuestionDto(q.getId(), q.getContent(), options, q.getCorrectAnswer());
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse options JSON", e);
            }
        }).collect(Collectors.toList());
        return ApiResponse.ok(dtos);
    }

    public record QuestionDto(Long id, String content, List<String> options, String correctAnswer) {}
}
