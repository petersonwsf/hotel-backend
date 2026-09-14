package com.hotel.hotel.config.rabbitmq;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableRabbit
@Configuration
public class RabbitMqConfig {

    @Value("${spring.rabbitmq.exchange}")
    public String exchange;
    @Value("${spring.rabbitmq.queue}")
    public String reservationQueue;

    @Bean
    public TopicExchange hotelExchange() {
        return new TopicExchange(this.exchange, true, false);
    }

    @Bean
    public Queue reservationPaymentQueue() {
        return QueueBuilder.durable(this.reservationQueue).build();
    }

    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {
        return new RabbitAdmin(connectionFactory);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        return new Jackson2JsonMessageConverter(objectMapper);
    }

    @Bean
    public Binding bindingPaymentEvents(Queue reservationPaymentQueue, TopicExchange hotelExchange) {
        return BindingBuilder
                .bind(reservationPaymentQueue)
                .to(hotelExchange)
                .with("payment.*");
    }
}
