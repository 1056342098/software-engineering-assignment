package com.schoolmanager.backend.student;

import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.profile.repo.StudentProfileRepository;
import com.schoolmanager.backend.profile.repo.StudentSensitiveRepository;
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
	private final StudentProfileRepository studentProfileRepository;
	private final StudentSensitiveRepository studentSensitiveRepository;

	public StudentService(StudentRepository studentRepository, ClassManagerRepository classManagerRepository,
			SysUserRepository sysUserRepository, SysRoleRepository sysRoleRepository, PasswordEncoder passwordEncoder,
			StudentProfileRepository studentProfileRepository, StudentSensitiveRepository studentSensitiveRepository) {
		this.studentRepository = studentRepository;
		this.classManagerRepository = classManagerRepository;
		this.sysUserRepository = sysUserRepository;
		this.sysRoleRepository = sysRoleRepository;
		this.passwordEncoder = passwordEncoder;
		this.studentProfileRepository = studentProfileRepository;
		this.studentSensitiveRepository = studentSensitiveRepository;
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
		Student student = studentRepository.findByStudentNo(req.studentNo()).orElse(null);
		SysUser user;
		boolean isNewUser = false;
		
		if (student != null) {
			user = student.getUser();
			if (user == null) {
				user = new SysUser();
				isNewUser = true;
			}
		} else {
			user = sysUserRepository.findByLoginName(req.studentNo()).orElseGet(SysUser::new);
			isNewUser = user.getId() == null;
			student = new Student();
		}

		user.setLoginName(user.getLoginName() == null ? req.studentNo() : user.getLoginName());
		user.setRealName(req.realName());
		if (isNewUser) {
			user.setPasswordHash(passwordEncoder.encode("123456"));
			user.setStatus((short) 1);
			SysRole studentRole = sysRoleRepository.findByCode("STUDENT").orElseThrow();
			user.getRoles().add(studentRole);
		}

		SysUser savedUser = sysUserRepository.save(user);

		student.setUser(savedUser);
		student.setStudentNo(req.studentNo());
		student.setMajor(req.major());
		student.setGrade(req.grade());
		student.setClassName(req.className());

		studentRepository.save(student);
	}

	@Transactional
	public void deleteStudent(long id) {
		studentSensitiveRepository.findByStudent_Id(id).ifPresent(studentSensitiveRepository::delete);
		studentProfileRepository.findFirstByStudent_Id(id).ifPresent(studentProfileRepository::delete);
		studentRepository.deleteById(id);
	}

	@Transactional
	public void importStudents(MultipartFile file) {
		try (InputStream is = file.getInputStream(); Workbook workbook = new XSSFWorkbook(is)) {
			Sheet sheet = workbook.getSheetAt(0);
			int successCount = 0;
			StringBuilder errors = new StringBuilder();

			for (int i = 1; i <= sheet.getLastRowNum(); i++) {
				Row row = sheet.getRow(i);
				if (row == null) continue;

				String studentNo = getCellValueAsString(row.getCell(0));
				String realName = getCellValueAsString(row.getCell(1));
				if (studentNo.isBlank() || realName.isBlank()) {
					errors.append(String.format("第%d行：学号或姓名为空；", i + 1));
					continue;
				}

				String major = getCellValueAsString(row.getCell(2));
				String gradeStr = getCellValueAsString(row.getCell(3));
				Integer grade = null;
				if (!gradeStr.isBlank()) {
					try {
						grade = Integer.parseInt(gradeStr.replace(".0", ""));
					} catch (NumberFormatException e) {
						errors.append(String.format("第%d行：年级格式不正确；", i + 1));
						continue;
					}
				}
				String className = getCellValueAsString(row.getCell(4));

				try {
					saveStudent(new StudentSaveRequest(studentNo, realName, major, grade, className));
					successCount++;
				} catch (Exception e) {
					errors.append(String.format("第%d行：保存失败(%s)；", i + 1, e.getMessage()));
				}
			}

			if (errors.length() > 0) {
				if (successCount == 0) {
					throw new com.schoolmanager.backend.common.ApiException(400, "导入全部失败: " + errors.toString());
				} else {
					throw new com.schoolmanager.backend.common.ApiException(400, "部分导入成功 (" + successCount + "条)。失败记录: " + errors.toString());
				}
			}
		} catch (Exception e) {
			if (e instanceof com.schoolmanager.backend.common.ApiException) {
				throw (com.schoolmanager.backend.common.ApiException) e;
			}
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			throw new RuntimeException("Excel导入处理异常: " + e.getMessage(), e);
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
