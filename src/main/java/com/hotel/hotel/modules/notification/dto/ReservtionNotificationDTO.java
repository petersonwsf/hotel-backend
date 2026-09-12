package com.hotel.hotel.modules.notification.dto;

import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.room.model.Category;

import java.time.LocalDate;

public record ReservtionNotificationDTO(
        Long id,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Category categoryRoom
) {
    public ReservtionNotificationDTO(Reservation reservation) {
        this(reservation.getId(), reservation.getCheckInDate(), reservation.getCheckOutDate(), reservation.getRoom().getCategory());
    }
}
