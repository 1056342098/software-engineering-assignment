package com.schoolmanager.backend.profile.repo;

import com.schoolmanager.backend.profile.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudentRepository extends JpaRepository<Student, Long> {
    List<Student> findByClassNameInOrderByIdAsc(List<String> classNames);
}
