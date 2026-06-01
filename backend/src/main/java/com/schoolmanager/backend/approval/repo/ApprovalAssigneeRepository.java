package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.ApprovalAssignee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ApprovalAssigneeRepository extends JpaRepository<ApprovalAssignee, Long> {
	@Query("select aa from ApprovalAssignee aa join fetch aa.approver where aa.approval.id = :approvalId order by aa.id asc")
	List<ApprovalAssignee> findByApprovalId(@Param("approvalId") Long approvalId);

	@Query("select aa from ApprovalAssignee aa where aa.approval.id = :approvalId and aa.approver.id = :approverId")
	Optional<ApprovalAssignee> findOne(@Param("approvalId") Long approvalId, @Param("approverId") Long approverId);

	@Query("select aa.approval.id from ApprovalAssignee aa where aa.approver.id = :approverId and aa.status = :status")
	List<Long> findApprovalIdsByApproverAndStatus(@Param("approverId") Long approverId, @Param("status") String status);

	@Query("select aa.approval.id from ApprovalAssignee aa where aa.approver.id = :approverId order by aa.approval.id desc")
	List<Long> findApprovalIdsByApprover(@Param("approverId") Long approverId);
}
