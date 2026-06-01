package com.schoolmanager.backend.oplog.repo;

import com.schoolmanager.backend.oplog.entity.OperationLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
}
