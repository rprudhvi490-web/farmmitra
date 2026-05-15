package com.weekendbasket.app.repository;

import com.weekendbasket.app.model.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId OR n.user IS NULL ORDER BY n.sentAt DESC")
    Page<Notification> findByUserIdOrBroadcast(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId OR n.user IS NULL ORDER BY n.sentAt DESC")
    List<Notification> findByUserIdOrBroadcast(@Param("userId") Long userId);

    long countByUserIdAndReadStatusFalse(Long userId);
}
