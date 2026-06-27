package com.schoolmanager.backend.notification.repo;

import com.schoolmanager.backend.notification.entity.NotificationRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRecipientRepository extends JpaRepository<NotificationRecipient, Long> {
	@Query("""
			select r from NotificationRecipient r
			join fetch r.notification n
			join fetch n.createdBy
			join fetch r.student s
			join fetch s.user
			where r.student.id = :studentId
			order by n.id desc
			""")
	List<NotificationRecipient> findInboxByStudentId(@Param("studentId") Long studentId);

	@Query("""
			select r from NotificationRecipient r
			join fetch r.student s
			join fetch s.user
			where r.notification.id = :notificationId
			order by r.id asc
			""")
	List<NotificationRecipient> findByNotificationIdWithStudent(@Param("notificationId") Long notificationId);

	@Query("""
			select r from NotificationRecipient r
			join fetch r.notification n
			where r.notification.id = :notificationId and r.student.id = :studentId
			""")
	Optional<NotificationRecipient> findByNotificationIdAndStudentId(
			@Param("notificationId") Long notificationId,
			@Param("studentId") Long studentId);
}
