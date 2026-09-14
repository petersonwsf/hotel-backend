package com.hotel.hotel.modules.rabbitmq.dtos;

import com.hotel.hotel.modules.rabbitmq.enums.ReservationEventType;

import java.time.Instant;
import java.util.UUID;

public record MessageDataEnvelope<T>(
    String eventId,
    ReservationEventType eventType,
    String eventVersion,
    String occurredAt,
    String source,
    String correlationId,
    T data
) {
    public static <T> MessageDataEnvelope<T> build(ReservationEventType eventType, T data, String correlationId) {
        return new MessageDataEnvelope<>(
            UUID.randomUUID().toString(),
            eventType,
            "1.0",
            Instant.now().toString(),
            "reservation-service",
            correlationId,
            data
        );
    };
}
