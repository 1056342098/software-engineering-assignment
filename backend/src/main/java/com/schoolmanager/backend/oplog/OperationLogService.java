package com.schoolmanager.backend.oplog;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.oplog.entity.OperationLog;
import com.schoolmanager.backend.oplog.repo.OperationLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class OperationLogService {
	private final OperationLogRepository repository;
	private final ObjectMapper objectMapper;

	public OperationLogService(OperationLogRepository repository, ObjectMapper objectMapper) {
		this.repository = repository;
		this.objectMapper = objectMapper;
	}

	public void log(long operatorId, String action, String targetType, Long targetId, Map<String, Object> detail) {
		OperationLog l = new OperationLog();
		l.setOperatorId(operatorId);
		l.setAction(action);
		l.setTargetType(targetType);
		l.setTargetId(targetId);
		if (detail != null) {
			try {
				l.setDetailJson(objectMapper.writeValueAsString(detail));
			} catch (Exception ignored) {
			}
		}
		repository.save(l);
	}
}
