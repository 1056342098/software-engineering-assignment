package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.ApprovalStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {
	List<ApprovalStep> findByApproval_IdOrderByStepNoAsc(Long approvalId);
}
