package com.schoolmanager.backend.student;

import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.student.repo.ClassManagerRepository;
import com.schoolmanager.backend.user.entity.SysRole;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysRoleRepository;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;

@Service
public class StudentService {
	private final StudentRepository studentRepository;
	private final ClassManagerRepository classManagerRepository;
	private final SysUserRepository sysUserRepository;
	private final SysRoleRepository sysRoleRepository;
	private final PasswordEncoder passwordEncoder;

	public StudentService(StudentRepository studentRepository, ClassManagerRepository classManagerRepository,
			SysUserRepository sysUserRepository, SysRoleRepository sysRoleRepository, PasswordEncoder passwordEncoder) {
		this.studentRepository = studentRepository;
		this.classManagerRepository = classManagerRepository;
		this.sysUserRepository = sysUserRepository;
		this.sysRoleRepository = sysRoleRepository;
		this.passwordEncoder = passwordEncoder;
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

	@Transactional
	public void saveStudent(StudentSaveRequest req) {
		SysUser user = sysUserRepository.findByLoginName(req.studentNo()).orElseGet(SysUser::new);
		boolean isNew = user.getId() == null;

		user.setLoginName(req.studentNo());
		user.setRealName(req.realName());
		if (isNew) {
			user.setPasswordHash(passwordEncoder.encode("123456"));
			user.setStatus((short) 1);
			SysRole studentRole = sysRoleRepository.findByCode("STUDENT").orElseThrow();
			user.getRoles().add(studentRole);
		}

		user = sysUserRepository.save(user);

		Student student = studentRepository.findById(user.getId()).orElseGet(() -> {
			Student s = new Student();
			s.setUser(user);
			return s;
		});

		student.setStudentNo(req.studentNo());
		student.setMajor(req.major());
		student.setGrade(req.grade());
		student.setClassName(req.className());

		studentRepository.save(student);
	}

	@Transactional
	public void importStudents(MultipartFile file) {
		try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
			Sheet sheet = workbook.getSheetAt(0);
			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null) continue;

				String studentNo = getCellValueAsString(row.getCell(0));
				String realName = getCellValueAsString(row.getCell(1));
				if (studentNo.isBlank() || realName.isBlank()) continue;

				String major = getCellValueAsString(row.getCell(2));
				String gradeStr = getCellValueAsString(row.getCell(3));
				Integer grade = null;
				if (!gradeStr.isBlank()) {
					try {
						grade = Integer.parseInt(gradeStr.replace(".0", ""));
					} catch (NumberFormatException e) {
						// ignore
					}
				}
				String className = getCellValueAsString(row.getCell(4));

				saveStudent(new StudentSaveRequest(studentNo, realName, major, grade, className));
			}
		} catch (Exception e) {
			throw new RuntimeException("Excel导入失败: " + e.getMessage(), e);
		}
	}

	private String getCellValueAsString(Cell cell) {
		if (cell == null) return "";
		return switch (cell.getCellType()) {
			case STRING -> cell.getStringCellValue().trim();
			case NUMERIC -> {
				double num = cell.getNumericCellValue();
				if (num == Math.floor(num)) {
					yield String.valueOf((long) num).trim();
				}
				yield String.valueOf(num).trim();
			}
			case BOOLEAN -> String.valueOf(cell.getBooleanCellValue()).trim();
			default -> "";
		};
	}

	public byte[] exportStudents(List<Student> students) {
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("学生信息");
			Row headerRow = sheet.createRow(0);
			headerRow.createCell(0).setCellValue("学号");
			headerRow.createCell(1).setCellValue("姓名");
			headerRow.createCell(2).setCellValue("专业");
			headerRow.createCell(3).setCellValue("年级");
			headerRow.createCell(4).setCellValue("班级");

			int rowNum = 1;
			for (Student s : students) {
				Row row = sheet.createRow(rowNum++);
				row.createCell(0).setCellValue(s.getStudentNo() != null ? s.getStudentNo() : "");
				row.createCell(1).setCellValue(s.getUser() != null && s.getUser().getRealName() != null ? s.getUser().getRealName() : "");
				row.createCell(2).setCellValue(s.getMajor() != null ? s.getMajor() : "");
				row.createCell(3).setCellValue(s.getGrade() != null ? s.getGrade().toString() : "");
				row.createCell(4).setCellValue(s.getClassName() != null ? s.getClassName() : "");
			}
			workbook.write(bos);
			return bos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Excel导出失败: " + e.getMessage(), e);
		}
	}
}
