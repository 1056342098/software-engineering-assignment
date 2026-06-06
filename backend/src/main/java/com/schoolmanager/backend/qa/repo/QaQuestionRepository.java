package com.schoolmanager.backend.qa.repo;

import com.schoolmanager.backend.qa.entity.QaQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QaQuestionRepository extends JpaRepository<QaQuestion, Long> {
    List<QaQuestion> findByType(String type);
}
