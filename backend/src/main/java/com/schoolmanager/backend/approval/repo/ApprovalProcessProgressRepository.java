package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.ApprovalProcessProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ApprovalProcessProgressRepository extends JpaRepository<ApprovalProcessProgress, Long> {
	Optional<ApprovalProcessProgress> findByUserIdAndApprovalType(Long userId, String approvalType);

	List<ApprovalProcessProgress> findByUserIdOrderByApprovalTypeAsc(Long userId);
}
