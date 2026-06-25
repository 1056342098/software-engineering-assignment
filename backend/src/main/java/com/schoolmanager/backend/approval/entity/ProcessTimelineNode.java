package com.schoolmanager.backend.approval.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "process_timeline_node")
public class ProcessTimelineNode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "approval_type", nullable = false, length = 32)
    private String approvalType;

    @Column(name = "stage_index", nullable = false)
    private Integer stageIndex;

    @Column(name = "stage_code", nullable = false, length = 32)
    private String stageCode;

    @Column(name = "stage_name", nullable = false, length = 64)
    private String stageName;

    @Column(name = "start_time")
    private Instant startTime;

    @Column(name = "end_time")
    private Instant endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getApprovalType() { return approvalType; }
    public void setApprovalType(String approvalType) { this.approvalType = approvalType; }
    public Integer getStageIndex() { return stageIndex; }
    public void setStageIndex(Integer stageIndex) { this.stageIndex = stageIndex; }
    public String getStageCode() { return stageCode; }
    public void setStageCode(String stageCode) { this.stageCode = stageCode; }
    public String getStageName() { return stageName; }
    public void setStageName(String stageName) { this.stageName = stageName; }
    public Instant getStartTime() { return startTime; }
    public void setStartTime(Instant startTime) { this.startTime = startTime; }
    public Instant getEndTime() { return endTime; }
    public void setEndTime(Instant endTime) { this.endTime = endTime; }
}
