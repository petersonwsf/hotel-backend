package com.hotel.hotel.modules.rabbitmq.dtos;

import java.math.BigDecimal;

public record PaymentDataBase(
    Long paymentId,
    Long reservationId,
    Long userId,
    String stripePaymentIntentId,
    BigDecimal amountAuthorized,
    BigDecimal amountCaptured,
    String currency,
    String status,
    String captureMethod,
    String createdAt,
    String updatedAt
) {}