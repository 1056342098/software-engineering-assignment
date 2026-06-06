package com.schoolmanager.backend.approval;

import com.schoolmanager.backend.approval.entity.ProcessTimelineNode;
import com.schoolmanager.backend.approval.repo.ProcessTimelineNodeRepository;
import com.schoolmanager.backend.common.ApiResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/timeline-config")
public class ProcessTimelineConfigController {

    private final ProcessTimelineNodeRepository timelineNodeRepository;

    public ProcessTimelineConfigController(ProcessTimelineNodeRepository timelineNodeRepository) {
        this.timelineNodeRepository = timelineNodeRepository;
    }

    @GetMapping("/{type}")
    public ApiResponse<List<ProcessTimelineNode>> getConfig(@PathVariable String type) {
        List<ProcessTimelineNode> nodes = timelineNodeRepository.findByApprovalTypeOrderByStageIndexAsc(type.toUpperCase());
        return ApiResponse.success(nodes);
    }

    @PutMapping("/{type}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('LEADER')")
    @Transactional
    public ApiResponse<Void> updateConfig(@PathVariable String type, @RequestBody List<ProcessTimelineNode> newNodes) {
        String t = type.toUpperCase();
        List<ProcessTimelineNode> existingNodes = timelineNodeRepository.findByApprovalTypeOrderByStageIndexAsc(t);
        
        // Simple update approach: update existing, add new, remove deleted
        // For simplicity and given the UI, we'll just delete all and insert new ones
        timelineNodeRepository.deleteAll(existingNodes);
        
        for (int i = 0; i < newNodes.size(); i++) {
            ProcessTimelineNode node = newNodes.get(i);
            ProcessTimelineNode toSave = new ProcessTimelineNode();
            toSave.setApprovalType(t);
            toSave.setStageIndex(i);
            toSave.setStageCode(node.getStageCode());
            toSave.setStageName(node.getStageName());
            toSave.setIntervalDays(node.getIntervalDays() == null ? 0 : node.getIntervalDays());
            timelineNodeRepository.save(toSave);
        }
        
        return ApiResponse.success(null);
    }
}
