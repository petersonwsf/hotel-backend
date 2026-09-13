package com.hotel.hotel.modules.rabbitmq.dtos;

public record PaymentMessageBroker(
    String eventId,
    String eventType,
    String eventVersion,
    String occurredAt,
    String source,
    String correlationId,
    PaymentDataBase data
) {
}
