package com.schoolmanager.backend.notification.repo;

import com.schoolmanager.backend.notification.entity.NotificationEmailConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationEmailConfigRepository extends JpaRepository<NotificationEmailConfig, Long> {
	Optional<NotificationEmailConfig> findByUser_Id(Long userId);
}
