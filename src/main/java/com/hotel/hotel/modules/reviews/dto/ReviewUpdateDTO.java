package com.hotel.hotel.modules.reviews.dto;

import jakarta.validation.constraints.Min;

public record ReviewUpdateDTO(
        String comment,
        @Min(value = 1, message = "Avaliação deve ter no mínimo 1 estrela")
        Double rating,
        String reply
) {}
