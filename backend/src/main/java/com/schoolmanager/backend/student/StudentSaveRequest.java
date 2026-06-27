package com.schoolmanager.backend.student;

import jakarta.validation.constraints.NotBlank;

public record StudentSaveRequest(
		Long id,
		@NotBlank(message = "学号不能为空") String studentNo,
		@NotBlank(message = "姓名不能为空") String realName,
		String major,
		Integer grade,
		String className
) {}
