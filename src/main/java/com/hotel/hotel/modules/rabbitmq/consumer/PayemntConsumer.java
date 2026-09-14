package com.hotel.hotel.modules.rabbitmq.consumer;

import com.hotel.hotel.modules.rabbitmq.dtos.PaymentMessageBroker;
import com.hotel.hotel.modules.reservation.service.ReservationService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class PayemntConsumer {

    @Autowired
    private ReservationService reservationService;

    @RabbitListener(queues = "${spring.rabbitmq.queue}")
    public void handlePayments(PaymentMessageBroker envelope) {

        if (envelope == null || envelope.data() == null) {
            log.warn("Received empty or invalid message payload");
            return;
        }
        String eventType = envelope.eventType();
        Long reservationId = envelope.data().reservationId();
        String correlationId = envelope.correlationId();

        log.info("Processing event '{}' for reservation ID: {} | correlationId: {}",
                eventType, reservationId, correlationId);

        switch (eventType) {
            case "payment.authorized":
            case "payment.captured":
                log.info("Confirming reservation with ID: {}", reservationId);
                reservationService.confirm(reservationId);
                break;

            default:
                log.debug("Event '{}' ignored by reservation domain", eventType);
                break;
        }
    }
}
