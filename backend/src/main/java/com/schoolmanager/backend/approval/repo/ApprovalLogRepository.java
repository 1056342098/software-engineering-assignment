package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.ApprovalLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalLogRepository extends JpaRepository<ApprovalLog, Long> {
	List<ApprovalLog> findByApproval_IdOrderByOpTimeAsc(Long approvalId);
}
