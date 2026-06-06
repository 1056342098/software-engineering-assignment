package com.schoolmanager.backend.approval.repo;

import com.schoolmanager.backend.approval.entity.ProcessTimelineNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcessTimelineNodeRepository extends JpaRepository<ProcessTimelineNode, Long> {
    List<ProcessTimelineNode> findByApprovalTypeOrderByStageIndexAsc(String approvalType);
}
