package com.schoolmanager.backend.student.repo;

import com.schoolmanager.backend.student.entity.ClassManager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ClassManagerRepository extends JpaRepository<ClassManager, ClassManager.Pk> {
	@Query("select cm.className from ClassManager cm where cm.userId = :userId")
	List<String> findClassNamesByUserId(@Param("userId") Long userId);
}
