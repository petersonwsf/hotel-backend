package com.hotel.hotel.modules.notification.repository;

import com.hotel.hotel.modules.notification.model.Notification;
import com.hotel.hotel.modules.notification.model.StatusNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserIdAndStatus(Long userId, StatusNotification status);
}
