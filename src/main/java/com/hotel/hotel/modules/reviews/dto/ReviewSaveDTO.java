package com.hotel.hotel.modules.reviews.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ReviewSaveDTO(
        @NotNull
        Double rating,
        @NotBlank
        String comment,
        @NotNull
        Long reservationId
) {}
