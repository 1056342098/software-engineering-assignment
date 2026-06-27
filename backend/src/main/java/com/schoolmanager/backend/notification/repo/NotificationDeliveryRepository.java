package com.schoolmanager.backend.notification.repo;

import com.schoolmanager.backend.notification.entity.NotificationDelivery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, Long> {
	@Query("""
			select d from NotificationDelivery d
			join fetch d.recipient r
			where r.notification.id = :notificationId
			order by r.id asc, d.id asc
			""")
	List<NotificationDelivery> findByNotificationId(@Param("notificationId") Long notificationId);

	@Query("""
			select d from NotificationDelivery d
			join fetch d.recipient r
			where r.id in :recipientIds
			order by r.id asc, d.id asc
			""")
	List<NotificationDelivery> findByRecipientIds(@Param("recipientIds") Collection<Long> recipientIds);
}
