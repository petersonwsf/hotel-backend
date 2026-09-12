package com.hotel.hotel.modules.notification.dto;

import com.hotel.hotel.modules.notification.model.Notification;
import com.hotel.hotel.modules.user.dtos.UserResponseDTO;

public record NotificationResponse(
        Long id,
        UserResponseDTO user,
        ReservtionNotificationDTO reservationInfos
) {
    public NotificationResponse(Notification notification) {
        this(
            notification.getId(),
            new UserResponseDTO(notification.getUser()),
            new ReservtionNotificationDTO(notification.getReservation())
        );
    }
}
