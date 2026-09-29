package com.hotel.hotel.modules.rabbitmq.dtos;

import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.reservation.model.Status;
import com.hotel.hotel.modules.user.model.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ReservationDataMessage(
        Long id,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal dailyRate,
        BigDecimal totalAmount,
        BigDecimal discountAmount,
        BigDecimal serviceFee,
        Status statusReservation,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Long userId,
        Long roomId,
        UserData user
) {
    public ReservationDataMessage(Reservation reservation, User user) {
        this(
                reservation.getId(),
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getDailyRate(),
                reservation.getTotalAmount(),
                reservation.getDiscountAmount(),
                reservation.getServiceFee(),
                reservation.getStatus(),
                reservation.getCreatedAt(),
                reservation.getUpdatedAt(),
                reservation.getUser().getId(),
                reservation.getRoom().getId(),
                new UserData(user)
        );
    }
}
