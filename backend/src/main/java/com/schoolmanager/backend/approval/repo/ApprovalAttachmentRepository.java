package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.ApprovalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ApprovalAttachmentRepository extends JpaRepository<ApprovalAttachment, Long> {
	@Query("select a from ApprovalAttachment a where a.approval.id = :approvalId order by a.id asc")
	List<ApprovalAttachment> findByApprovalId(@Param("approvalId") Long approvalId);
}

