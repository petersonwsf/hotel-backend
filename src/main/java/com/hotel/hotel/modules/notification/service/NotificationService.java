package com.hotel.hotel.modules.notification.service;

import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.modules.notification.dto.NotificationResponse;
import com.hotel.hotel.modules.notification.model.Notification;
import com.hotel.hotel.modules.notification.model.StatusNotification;
import com.hotel.hotel.modules.notification.repository.NotificationRepository;
import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
public class NotificationService {

    @Autowired
    private NotificationRepository repository;

    @Transactional
    public void create(User user, Reservation reservation) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setReservation(reservation);
        notification.setStatus(StatusNotification.PENDING);
        repository.save(notification);
    }

    public List<NotificationResponse> getPendingsNotificationsUser(Long userId) {
        return repository.findByUserIdAndStatus(userId, StatusNotification.PENDING)
                .stream()
                .map(NotificationResponse::new).toList();
    }

    @Transactional
    @PreAuthorize("@securityHelper.hasUserNotificationPermission(#id)")
    public void updateStatus(Long id, StatusNotification status) {
        Notification notification = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        notification.setStatus(status);
    }

}
