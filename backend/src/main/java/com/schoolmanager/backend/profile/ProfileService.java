package com.schoolmanager.backend.profile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.crypto.AesCryptoService;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.entity.StudentProfile;
import com.schoolmanager.backend.profile.entity.StudentSensitive;
import com.schoolmanager.backend.profile.entity.UserProfile;
import com.schoolmanager.backend.profile.repo.StudentProfileRepository;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.profile.repo.StudentSensitiveRepository;
import com.schoolmanager.backend.profile.repo.UserProfileRepository;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileService {
	private final StudentRepository studentRepository;
	private final StudentProfileRepository profileRepository;
	private final StudentSensitiveRepository sensitiveRepository;
	private final UserProfileRepository userProfileRepository;
	private final SysUserRepository userRepository;
	private final AesCryptoService cryptoService;
	private final ObjectMapper objectMapper;
	private final OperationLogService opLogService;

	public ProfileService(
			StudentRepository studentRepository,
			StudentProfileRepository profileRepository,
			StudentSensitiveRepository sensitiveRepository,
			UserProfileRepository userProfileRepository,
			SysUserRepository userRepository,
			AesCryptoService cryptoService,
			ObjectMapper objectMapper,
			OperationLogService opLogService) {
		this.studentRepository = studentRepository;
		this.profileRepository = profileRepository;
		this.sensitiveRepository = sensitiveRepository;
		this.userProfileRepository = userProfileRepository;
		this.userRepository = userRepository;
		this.cryptoService = cryptoService;
		this.objectMapper = objectMapper;
		this.opLogService = opLogService;
	}

	public Map<String, Object> getProfile(long requesterId, boolean requesterIsAdmin, long studentId) {
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ApiException(404, "未找到该学生"));

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("kind", "STUDENT");
		out.put("userId", student.getId());
		out.put("loginName", student.getUser().getLoginName());
		out.put("email", student.getUser().getEmail());
		out.put("studentId", student.getId());
		out.put("studentNo", student.getStudentNo());
		out.put("major", student.getMajor());
		out.put("grade", student.getGrade());
		out.put("className", student.getClassName());
		out.put("realName", student.getUser().getRealName());

		String publicJson = null;
		UserProfile up = userProfileRepository.findByUser_Id(studentId).orElse(null);
		if (up != null && up.getPublicJson() != null && !up.getPublicJson().isBlank()) {
			publicJson = up.getPublicJson();
		} else {
			Optional<StudentProfile> sp = profileRepository.findFirstByStudent_Id(studentId);
			if (sp.isPresent() && sp.get().getPublicJson() != null && !sp.get().getPublicJson().isBlank()) {
				publicJson = sp.get().getPublicJson();
			}
		}

		if (publicJson != null && !publicJson.isBlank()) {
			try {
				out.put("public", objectMapper.readValue(publicJson, Map.class));
			} catch (Exception e) {
				out.put("public", Map.of());
			}
		} else {
			out.put("public", Map.of());
		}

		boolean canSeeSensitive = requesterIsAdmin || requesterId == studentId;
		if (canSeeSensitive) {
			StudentSensitive ss = sensitiveRepository.findByStudent_Id(studentId).orElse(null);
			if (ss == null) {
				out.put("sensitive", null);
			} else {
				Map<String, Object> s = new LinkedHashMap<>();
				s.put("idCardNo", cryptoService.decryptFromBase64(ss.getIdCardNoEnc()));
				s.put("hukouAddr", cryptoService.decryptFromBase64(ss.getHukouAddrEnc()));
				s.put("hometown", cryptoService.decryptFromBase64(ss.getHometownEnc()));
				s.put("tutor", cryptoService.decryptFromBase64(ss.getTutorEnc()));
				s.put("delayInfo", cryptoService.decryptFromBase64(ss.getDelayInfoEnc()));
				out.put("sensitive", s);
			}
		} else {
			out.put("sensitive", Map.of("masked", true));
		}

		return out;
	}

	public Map<String, Object> getUserProfile(long requesterId, boolean requesterIsAdmin, long userId) {
		SysUser u = userRepository.findById(userId).orElseThrow(() -> new ApiException(404, "未找到用户"));

		Map<String, Object> out = new LinkedHashMap<>();
		out.put("kind", "USER");
		out.put("userId", u.getId());
		out.put("loginName", u.getLoginName());
		out.put("email", u.getEmail());
		out.put("realName", u.getRealName());
		out.put("studentId", null);
		out.put("studentNo", null);
		out.put("major", null);
		out.put("grade", null);
		out.put("className", null);

		UserProfile up = userProfileRepository.findByUser_Id(userId).orElse(null);
		if (up != null && up.getPublicJson() != null && !up.getPublicJson().isBlank()) {
			try {
				out.put("public", objectMapper.readValue(up.getPublicJson(), Map.class));
			} catch (Exception e) {
				out.put("public", Map.of());
			}
		} else {
			out.put("public", Map.of());
		}

		out.put("sensitive", requesterIsAdmin || requesterId == userId ? null : Map.of("masked", true));
		return out;
	}

	@Transactional
	public void upsertPublicProfile(long userId, Map<String, Object> publicPart) {
		SysUser user = userRepository.findById(userId).orElseThrow(() -> new ApiException(404, "未找到用户"));
		UserProfile up = userProfileRepository.findByUser_Id(userId).orElseGet(UserProfile::new);
		up.setUser(user);
		try {
			up.setPublicJson(objectMapper.writeValueAsString(publicPart));
		} catch (Exception e) {
			throw new ApiException(400, "公开信息格式无效");
		}
		userProfileRepository.save(up);
		opLogService.log(userId, "PROFILE_PUBLIC_UPSERT", "user_profile", up.getId(), null);
	}

	@Transactional
	public void upsertSensitive(long operatorId, long studentId, Map<String, Object> sensitivePart) {
		Student student = studentRepository.findById(studentId)
				.orElseThrow(() -> new ApiException(404, "未找到该学生"));
		StudentSensitive ss = sensitiveRepository.findByStudent_Id(studentId).orElseGet(StudentSensitive::new);
		ss.setStudent(student);
		ss.setUpdatedBy(operatorId);

		ss.setIdCardNoEnc(cryptoService.encryptToBase64(asStr(sensitivePart.get("idCardNo"))));
		ss.setHukouAddrEnc(cryptoService.encryptToBase64(asStr(sensitivePart.get("hukouAddr"))));
		ss.setHometownEnc(cryptoService.encryptToBase64(asStr(sensitivePart.get("hometown"))));
		ss.setTutorEnc(cryptoService.encryptToBase64(asStr(sensitivePart.get("tutor"))));
		ss.setDelayInfoEnc(cryptoService.encryptToBase64(asStr(sensitivePart.get("delayInfo"))));

		sensitiveRepository.save(ss);
		opLogService.log(operatorId, "PROFILE_SENSITIVE_UPSERT", "student_sensitive", ss.getId(),
				Map.of("studentId", studentId));
	}

	private static String asStr(Object v) {
		return v == null ? null : String.valueOf(v);
	}
}
