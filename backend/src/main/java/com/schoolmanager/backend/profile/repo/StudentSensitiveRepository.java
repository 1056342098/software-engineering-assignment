package com.schoolmanager.backend.profile.repo;

import com.schoolmanager.backend.profile.entity.StudentSensitive;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentSensitiveRepository extends JpaRepository<StudentSensitive, Long> {
	Optional<StudentSensitive> findByStudent_Id(Long studentId);
}
