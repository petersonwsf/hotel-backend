package com.hotel.hotel.modules.rabbitmq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ReservationEventType {
    RESERVATION_CANCELLED("reservation.cancelled");

    private final String value;

    ReservationEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
