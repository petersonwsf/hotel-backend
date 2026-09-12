package com.hotel.hotel.modules.reviews.dto;

import com.hotel.hotel.modules.reviews.model.Sentiment;

import java.time.LocalDate;
import java.util.List;

public record ReviewQueryParams(
        Long userId,
        Long roomId,
        Boolean replied,
        Boolean commented,
        List<Sentiment> sentiment,
        LocalDate createdAt
) {}
