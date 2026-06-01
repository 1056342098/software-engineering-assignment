package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.Approval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalRepository extends JpaRepository<Approval, Long> {
	@Query("select a from Approval a join fetch a.applicant where a.applicant.id = :applicantId order by a.id desc")
	List<Approval> findByApplicant_IdOrderByIdDesc(@Param("applicantId") Long applicantId);

	List<Approval> findByApprover_IdAndStatusOrderByIdDesc(Long approverId, String status);

	List<Approval> findByApprover_IdOrderByIdDesc(Long approverId);

	@Query("select a from Approval a join fetch a.applicant where a.id in :ids order by a.id desc")
	List<Approval> findByIdInWithApplicant(@Param("ids") List<Long> ids);

	@Query("select a from Approval a join fetch a.applicant where a.id = :id")
	Optional<Approval> findByIdWithApplicant(@Param("id") Long id);

	boolean existsByApplicant_IdAndTypeAndStatus(Long applicantId, String type, String status);

	Optional<Approval> findFirstByApplicant_IdAndTypeAndStatusOrderByIdDesc(Long applicantId, String type,
			String status);
}
