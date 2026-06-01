package com.schoolmanager.backend.student;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.security.CurrentUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/students")
public class StudentController {
	private final StudentService studentService;
	private final CurrentUser currentUser;

	public StudentController(StudentService studentService, CurrentUser currentUser) {
		this.studentService = studentService;
		this.currentUser = currentUser;
	}

	@GetMapping
	@PreAuthorize("hasAnyRole('LEADER','TEACHER','CADRE')")
	public ApiResponse<List<StudentDto>> list() {
		if (currentUser.hasRole("LEADER")) {
			return ApiResponse.ok(studentService.listAllStudents().stream().map(StudentDto::from).toList());
		}
		return ApiResponse.ok(studentService.listManagedStudents(currentUser.id()).stream().map(StudentDto::from).toList());
	}

	public record StudentDto(Long id, String realName, String studentNo, String major, Integer grade, String className) {
		static StudentDto from(Student s) {
			return new StudentDto(
					s.getId(),
					s.getUser() == null ? null : s.getUser().getRealName(),
					s.getStudentNo(),
					s.getMajor(),
					s.getGrade(),
					s.getClassName()
			);
		}
	}
}
