package com.hotel.hotel.modules.rabbitmq.service;

import com.hotel.hotel.config.rabbitmq.RabbitMqConfig;
import com.hotel.hotel.modules.rabbitmq.dtos.ReservationDataEnvelope;
import com.hotel.hotel.modules.rabbitmq.enums.PaymentEventsTypes;
import com.hotel.hotel.modules.rabbitmq.enums.ReservationEventType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class ReservationEventPublisher {

    private final RabbitTemplate rabbitTemplate;
    private final TopicExchange hotelExchange;

    public ReservationEventPublisher(RabbitTemplate rabbitTemplate, TopicExchange hotelExchange) {
        this.rabbitTemplate = rabbitTemplate;
        this.hotelExchange = hotelExchange;
    }

    public <T> void publishEvent(ReservationEventType eventType, T eventData, String correlationId) {
        ReservationDataEnvelope<T> envelope = ReservationDataEnvelope.build(eventType, eventData, correlationId);
        String routingKey = eventType.getValue();
        Map<String, Object> payload = Map.of(
                "pattern", eventType.getValue(),
                "data", envelope
        );
        rabbitTemplate.convertAndSend(
                this.hotelExchange.getName(),
                routingKey,
                payload
        );
        log.info("Evento publicado com sucesso: " + routingKey);
    }
}
