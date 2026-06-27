package com.schoolmanager.backend.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.notification.repo.NotificationDeliveryRepository;
import com.schoolmanager.backend.notification.repo.NotificationRecipientRepository;
import com.schoolmanager.backend.notification.repo.NotificationRepository;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.profile.entity.Student;
import com.schoolmanager.backend.profile.repo.StudentRepository;
import com.schoolmanager.backend.student.repo.ClassManagerRepository;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {
	@Mock
	private NotificationRepository notificationRepository;

	@Mock
	private NotificationRecipientRepository recipientRepository;

	@Mock
	private NotificationDeliveryRepository deliveryRepository;

	@Mock
	private StudentRepository studentRepository;

	@Mock
	private ClassManagerRepository classManagerRepository;

	@Mock
	private SysUserRepository userRepository;

	@Mock
	private NotificationEmailSettingsService emailSettingsService;

	@Mock
	private NotificationEmailGateway notificationEmailGateway;

	@Mock
	private OperationLogService opLogService;

	private NotificationService notificationService;

	@BeforeEach
	void setUp() {
		notificationService = new NotificationService(
				notificationRepository,
				recipientRepository,
				deliveryRepository,
				studentRepository,
				classManagerRepository,
				userRepository,
				emailSettingsService,
				notificationEmailGateway,
				new ObjectMapper(),
				opLogService);
	}

	@Test
	void shouldFailFastWhenEmailChannelSelectedWithoutSenderConfig() {
		SysUser creator = user(1L, "teacher1", "老师", "teacher@example.com");
		Student student = new Student();
		student.setUser(user(11L, "student1", "张同学", "student@example.com"));

		when(userRepository.findById(1L)).thenReturn(Optional.of(creator));
		when(studentRepository.findAll()).thenReturn(List.of(student));
		when(emailSettingsService.requireResolvedSender(1L))
				.thenThrow(new ApiException(400, "已选择邮件渠道，请先在个人资料中配置发件邮箱和 SMTP 信息"));

		ApiException ex = assertThrows(ApiException.class, () -> notificationService.create(
				1L,
				Set.of("LEADER"),
				new NotificationService.CreateCommand(
						"测试通知",
						"正文",
						null,
						null,
						null,
						List.of(),
						List.of("EMAIL"),
						null)));

		assertEquals(400, ex.getCode());
		assertEquals("已选择邮件渠道，请先在个人资料中配置发件邮箱和 SMTP 信息", ex.getMessage());
	}

	private static SysUser user(Long id, String loginName, String realName, String email) {
		SysUser user = new SysUser();
		user.setId(id);
		user.setLoginName(loginName);
		user.setRealName(realName);
		user.setEmail(email);
		return user;
	}
}
