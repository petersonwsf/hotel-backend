package com.hotel.hotel.modules.rabbitmq.enums;

import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentEventsTypes {
    PAYMENT_CREATED("payment.created"),
    PAYMENT_REQUIRES_ACTION("payment.requires_action"),
    PAYMENT_AUTHORIZED("payment.authorized"),
    PAYMENT_CAPTURED("payment.captured"),
    PAYMENT_CANCELED("payment.canceled"),
    PAYMENT_FAILED("payment.failed"),
    PAYMENT_REFUNDED("payment.refunded"),
    BOLETO_GENERATED("boleto.generated");

    private final String value;

    PaymentEventsTypes(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
