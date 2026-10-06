package com.hotel.hotel.modules.rabbitmq.consumer;

import com.hotel.hotel.modules.rabbitmq.dtos.MessageWrapper;
import com.hotel.hotel.modules.rabbitmq.dtos.PaymentMessageBroker;
import com.hotel.hotel.modules.reservation.service.ReservationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Consumidor de eventos de pagamento via RabbitMQ.
 * <p>
 * Exceções são sempre capturadas e logadas; nunca propagadas
 * (o AMQP framework marcaria a mensagem como nack/requeue).
 */
@Slf4j
@Component
public class PaymentConsumer {

    @Autowired
    private ReservationService reservationService;

    @RabbitListener(queues = "${spring.rabbitmq.queue}")
    public void handlePayments(MessageWrapper wrapper) {
        PaymentMessageBroker envelope = wrapper.data();
        if (envelope == null || envelope.data() == null) {
            log.warn("Received empty or invalid message payload");
            return;
        }

        String eventType = envelope.eventType();
        Long reservationId = envelope.data().reservationId();
        String correlationId = envelope.correlationId();

        log.info("Processing event '{}' for reservation ID: {} | correlationId: {}",
                eventType, reservationId, correlationId);

        try {
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
        } catch (Exception ex) {
            // Exceções em consumidores @RabbitListener não chegam ao front-end,
            // mas precisam ser logadas com nível error para visibilidade.
            log.error("Falha ao processar evento '{}' para reserva ID: {} | correlationId: {}",
                    eventType, reservationId, correlationId, ex);
            // Re-throw para que o RabbitMQ possa fazer requeue / DLQ conforme configuração
            throw ex;
        }
    }
}