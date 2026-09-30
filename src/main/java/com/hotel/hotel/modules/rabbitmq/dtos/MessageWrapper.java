package com.hotel.hotel.modules.rabbitmq.dtos;

public record MessageWrapper(
        String pattern,
        PaymentMessageBroker data
) {
}
