package com.hotel.hotel.modules.room.dtos;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;

import com.hotel.hotel.modules.reviews.dto.RoomRatingSummaryDTO;
import com.hotel.hotel.modules.room.model.Category;
import com.hotel.hotel.modules.room.model.Room;
import com.hotel.hotel.modules.room.model.StatusRoom;

public record RoomDetailsImageDTO(
        Long id,
        String code,
        String floor,
        BigDecimal customPrice,
        Boolean active,
        StatusRoom status,
        String description,
        List<String> amenities,
        Integer capacity,
        Category category,
        List<String> image,
        RoomRatingSummaryDTO ratingSummary
) {
    public RoomDetailsImageDTO(Room room, List<String> image, RoomRatingSummaryDTO ratingSummary) {
        this(
                room.getId(),
                room.getCode(),
                room.getFloor(),
                room.getCustomPrice(),
                room.getActive(),
                room.getStatus(),
                room.getDescription(),
                Arrays.asList(room.getAmenities()),
                room.getCapacity(),
                room.getCategory(),
                image,
                ratingSummary
        );
    }
}
