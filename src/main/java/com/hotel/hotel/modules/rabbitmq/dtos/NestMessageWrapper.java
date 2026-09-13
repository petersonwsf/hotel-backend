package com.hotel.hotel.modules.rabbitmq.dtos;

public record NestMessageWrapper<T>(
    String pattern,
    T data
) {}
