package com.schoolmanager.backend.policy.repo;

import com.schoolmanager.backend.policy.entity.PolicyDoc;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PolicyDocRepository extends JpaRepository<PolicyDoc, Long> {
	@Query("select d from PolicyDoc d join fetch d.uploader where d.status = :status order by d.id desc")
	List<PolicyDoc> findByStatusOrderByIdDesc(@Param("status") String status);

	@Query("select d from PolicyDoc d join fetch d.uploader where d.uploader.id = :uploaderId order by d.id desc")
	List<PolicyDoc> findByUploader_IdOrderByIdDesc(@Param("uploaderId") Long uploaderId);

	@Query("select d from PolicyDoc d join fetch d.uploader where d.id = :id")
	java.util.Optional<PolicyDoc> findByIdWithUploader(@Param("id") Long id);
}
