package com.schoolmanager.backend.notification.repo;

import com.schoolmanager.backend.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
	@Query("select n from Notification n join fetch n.createdBy order by n.id desc")
	List<Notification> findAllWithCreatorOrderByIdDesc();

	@Query("select n from Notification n join fetch n.createdBy where n.createdBy.id = :creatorId order by n.id desc")
	List<Notification> findByCreatedByIdWithCreatorOrderByIdDesc(@Param("creatorId") Long creatorId);

	@Query("select n from Notification n join fetch n.createdBy where n.id = :id")
	Optional<Notification> findByIdWithCreator(@Param("id") Long id);
}
