package com.hotel.hotel.modules.room.dtos;

import java.math.BigDecimal;
import java.util.List;

import com.hotel.hotel.modules.room.model.Category;
import com.hotel.hotel.modules.room.model.StatusRoom;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RoomEditDTO(
    StatusRoom status,
    @DecimalMin("100.00")
    BigDecimal customPrice,
    Long roomType,
    List<String> amenities,
    @Min(value = 1)
    Integer capacity,
    Category category,
    List<String> remainingImages,
    @NotNull
    String description
) {
}
