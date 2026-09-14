package com.hotel.hotel.modules.rabbitmq.dtos;

import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.reservation.model.Status;
import com.hotel.hotel.modules.user.dtos.UserResponseDTO;
import com.hotel.hotel.modules.user.model.User;

public record ReservationDataMessage(
        Long id,
        UserResponseDTO user
) {
    public ReservationDataMessage(Reservation reservation, User user) {
        this(reservation.getId(), new UserResponseDTO(user));
    }
}
