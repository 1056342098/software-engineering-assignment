package com.schoolmanager.backend.init;

import com.schoolmanager.backend.crypto.AesCryptoService;
import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.entity.StudentProfile;
import com.schoolmanager.backend.profile.entity.StudentSensitive;
import com.schoolmanager.backend.profile.repo.StudentProfileRepository;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.profile.repo.StudentSensitiveRepository;
import com.schoolmanager.backend.student.entity.ClassManager;
import com.schoolmanager.backend.student.repo.ClassManagerRepository;
import com.schoolmanager.backend.user.entity.SysRole;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysRoleRepository;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
public class DataInitializer implements CommandLineRunner {
	private final SysRoleRepository roleRepository;
	private final SysUserRepository userRepository;
	private final StudentRepository studentRepository;
	private final StudentProfileRepository studentProfileRepository;
	private final StudentSensitiveRepository studentSensitiveRepository;
	private final ClassManagerRepository classManagerRepository;
	private final PasswordEncoder passwordEncoder;
	private final AesCryptoService cryptoService;

	public DataInitializer(
			SysRoleRepository roleRepository,
			SysUserRepository userRepository,
			StudentRepository studentRepository,
			StudentProfileRepository studentProfileRepository,
			StudentSensitiveRepository studentSensitiveRepository,
			ClassManagerRepository classManagerRepository,
			PasswordEncoder passwordEncoder,
			AesCryptoService cryptoService) {
		this.roleRepository = roleRepository;
		this.userRepository = userRepository;
		this.studentRepository = studentRepository;
		this.studentProfileRepository = studentProfileRepository;
		this.studentSensitiveRepository = studentSensitiveRepository;
		this.passwordEncoder = passwordEncoder;
		this.classManagerRepository = classManagerRepository;
		this.cryptoService = cryptoService;
	}

	@Override
	@Transactional
	public void run(String... args) {
		SysRole leader = ensureRole("LEADER", "学院领导", (short) 1);
		SysRole teacher = ensureRole("TEACHER", "管理老师", (short) 2);
		SysRole cadre = ensureRole("CADRE", "班团骨干", (short) 3);
		SysRole studentRole = ensureRole("STUDENT", "普通学生", (short) 4);

		SysUser u1 = ensureUser("leader1", "学院领导", "leader1@example.com", leader);
		SysUser u2 = ensureUser("teacher1", "管理老师", "teacher1@example.com", teacher);
		SysUser u3 = ensureUser("cadre1", "班团骨干", "cadre1@example.com", cadre);
		SysUser u4 = ensureUser("student1", "张同学", "student1@example.com", studentRole);

		Student s = studentRepository.findById(u4.getId()).orElseGet(Student::new);
		s.setUser(u4);
		s.setStudentNo(s.getStudentNo() == null ? "2026123456" : s.getStudentNo());
		s.setMajor(s.getMajor() == null ? "计算机科学与技术" : s.getMajor());
		s.setGrade(s.getGrade() == null ? 2026 : s.getGrade());
		s.setClassName(s.getClassName() == null ? "2026-1班" : s.getClassName());
		studentRepository.save(s);

		if (studentProfileRepository.findFirstByStudent_Id(u4.getId()).isEmpty()) {
			StudentProfile sp = new StudentProfile();
			sp.setStudent(s);
			sp.setPublicJson(
					"""
							{"competitions":[{"name":"程序设计竞赛","level":"院级","year":2026}],"practices":[{"name":"志愿服务","hours":12}]}
							"""
							.strip());
			studentProfileRepository.save(sp);
		}

		if (studentSensitiveRepository.findByStudent_Id(u4.getId()).isEmpty()) {
			StudentSensitive ss = new StudentSensitive();
			ss.setStudent(s);
			ss.setUpdatedBy(u2.getId());
			ss.setIdCardNoEnc(cryptoService.encryptToBase64("110101202601010000"));
			ss.setHometownEnc(cryptoService.encryptToBase64("北京"));
			ss.setHukouAddrEnc(cryptoService.encryptToBase64("北京市海淀区"));
			ss.setTutorEnc(cryptoService.encryptToBase64("李老师"));
			ss.setDelayInfoEnc(cryptoService.encryptToBase64("无"));
			studentSensitiveRepository.save(ss);
		}

		ensureClassManager(u2.getId(), "2026-1班");
		ensureClassManager(u3.getId(), "2026-1班");
	}

	private SysRole ensureRole(String code, String name, short level) {
		return roleRepository.findByCode(code).orElseGet(() -> {
			SysRole r = new SysRole();
			r.setCode(code);
			r.setName(name);
			r.setLevel(level);
			return roleRepository.save(r);
		});
	}

	private SysUser createUser(String loginName, String realName, String email, SysRole role) {
		SysUser u = new SysUser();
		u.setLoginName(loginName);
		u.setRealName(realName);
		u.setEmail(email);
		u.setPasswordHash(passwordEncoder.encode("123456"));
		u.getRoles().add(role);
		return userRepository.save(u);
	}

	private SysUser ensureUser(String loginName, String realName, String email, SysRole role) {
		return userRepository.findByLoginName(loginName).orElseGet(() -> createUser(loginName, realName, email, role));
	}

	private void ensureClassManager(Long userId, String className) {
		ClassManager.Pk pk = new ClassManager.Pk(userId, className);
		if (classManagerRepository.existsById(pk)) {
			return;
		}
		ClassManager cm = new ClassManager();
		cm.setUserId(userId);
		cm.setClassName(className);
		classManagerRepository.save(cm);
	}
}
