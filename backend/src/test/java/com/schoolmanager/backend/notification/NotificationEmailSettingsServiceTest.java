package com.schoolmanager.backend.notification;

import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.crypto.AesCryptoService;
import com.schoolmanager.backend.notification.entity.NotificationEmailConfig;
import com.schoolmanager.backend.notification.repo.NotificationEmailConfigRepository;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationEmailSettingsServiceTest {
	@Mock
	private SysUserRepository userRepository;

	@Mock
	private NotificationEmailConfigRepository configRepository;

	@Mock
	private AesCryptoService cryptoService;

	@Mock
	private OperationLogService opLogService;

	@InjectMocks
	private NotificationEmailSettingsService service;

	@Test
	void shouldRejectSenderConfigForStudentRole() {
		ApiException ex = assertThrows(ApiException.class, () -> service.upsertSenderSettings(
				1L,
				Set.of("STUDENT"),
				new NotificationEmailSettingsService.UpdateSenderCommand(
						"sender@example.com",
						"老师",
						"smtp.example.com",
						587,
						"sender@example.com",
						"secret",
						true,
						false)));

		assertEquals(403, ex.getCode());
		verify(userRepository, never()).findById(any());
	}

	@Test
	void shouldKeepExistingEncryptedPasswordWhenPasswordIsBlank() {
		SysUser user = user(9L);
		NotificationEmailConfig config = new NotificationEmailConfig();
		config.setUser(user);
		config.setSenderEmail("old@example.com");
		config.setSenderName("旧老师");
		config.setSmtpHost("smtp.old.com");
		config.setSmtpPort(465);
		config.setSmtpUsername("old-user");
		config.setSmtpPasswordEnc("encrypted-old");
		config.setStarttlsEnabled(false);
		config.setSslEnabled(true);

		when(userRepository.findById(9L)).thenReturn(Optional.of(user));
		when(configRepository.findByUser_Id(9L)).thenReturn(Optional.of(config));
		when(configRepository.save(any(NotificationEmailConfig.class))).thenAnswer(invocation -> invocation.getArgument(0));

		service.upsertSenderSettings(
				9L,
				Set.of("TEACHER"),
				new NotificationEmailSettingsService.UpdateSenderCommand(
						"sender@example.com",
						"新老师",
						"smtp.example.com",
						587,
						"sender@example.com",
						"   ",
						true,
						false));

		assertEquals("encrypted-old", config.getSmtpPasswordEnc());
		assertEquals("sender@example.com", config.getSenderEmail());
		assertEquals("smtp.example.com", config.getSmtpHost());
	}

	private static SysUser user(Long id) {
		SysUser user = new SysUser();
		user.setId(id);
		user.setLoginName("teacher");
		user.setRealName("老师");
		return user;
	}
}
