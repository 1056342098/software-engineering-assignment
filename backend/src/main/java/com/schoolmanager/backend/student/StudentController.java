package com.schoolmanager.backend.student;

import com.schoolmanager.backend.common.ApiResponse;
import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.security.CurrentUser;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

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

	@PostMapping
	@PreAuthorize("hasAnyRole('LEADER','TEACHER')")
	public ApiResponse<Void> createOrUpdateStudent(@Valid @RequestBody StudentSaveRequest request) {
		studentService.saveStudent(request);
		return ApiResponse.ok(null);
	}

	@PostMapping("/import")
	@PreAuthorize("hasRole('LEADER')")
	public ApiResponse<Void> importStudents(@RequestParam("file") MultipartFile file) {
		studentService.importStudents(file);
		return ApiResponse.ok(null);
	}

	@DeleteMapping("/{id}")
	@PreAuthorize("hasRole('LEADER')")
	public ApiResponse<Void> deleteStudent(@PathVariable("id") long id) {
		studentService.deleteStudent(id);
		return ApiResponse.ok(null);
	}

	@GetMapping("/export")
	@PreAuthorize("hasAnyRole('LEADER','TEACHER','CADRE')")
	public ResponseEntity<byte[]> exportStudents() {
		List<Student> students;
		if (currentUser.hasRole("LEADER")) {
			students = studentService.listAllStudents();
		} else {
			students = studentService.listManagedStudents(currentUser.id());
		}
		byte[] data = studentService.exportStudents(students);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"students.xlsx\"")
				.contentType(MediaType.APPLICATION_OCTET_STREAM)
				.body(data);
	}
}
