package com.schoolmanager.backend.profile.repo;

import com.schoolmanager.backend.profile.entity.StudentProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
	Optional<StudentProfile> findFirstByStudent_Id(Long studentId);
}
