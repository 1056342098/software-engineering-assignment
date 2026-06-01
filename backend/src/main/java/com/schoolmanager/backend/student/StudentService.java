package com.schoolmanager.backend.student;

import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.student.repo.ClassManagerRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StudentService {
	private final StudentRepository studentRepository;
	private final ClassManagerRepository classManagerRepository;

	public StudentService(StudentRepository studentRepository, ClassManagerRepository classManagerRepository) {
		this.studentRepository = studentRepository;
		this.classManagerRepository = classManagerRepository;
	}

	public List<Student> listAllStudents() {
		return studentRepository.findAll().stream().sorted((a, b) -> a.getId().compareTo(b.getId())).toList();
	}

	public List<Student> listManagedStudents(long userId) {
		List<String> classNames = classManagerRepository.findClassNamesByUserId(userId);
		if (classNames.isEmpty()) {
			return List.of();
		}
		return studentRepository.findByClassNameInOrderByIdAsc(classNames);
	}
}
