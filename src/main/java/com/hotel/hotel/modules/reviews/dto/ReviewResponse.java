package com.hotel.hotel.modules.reviews.dto;

import com.hotel.hotel.modules.contactInformation.dtos.ContactInformationDTO;
import com.hotel.hotel.modules.reviews.model.Review;
import com.hotel.hotel.modules.reviews.model.Sentiment;
import com.hotel.hotel.modules.room.model.Category;
import com.hotel.hotel.modules.user.dtos.UserResponseDTO;

import java.time.LocalDateTime;

public record ReviewResponse(
        Long id,
        Double rating,
        String comment,
        Sentiment sentiment,
        UserResponseDTO user,
        ContactInformationDTO address,
        Category categoryRoom,
        String reply,
        LocalDateTime repliedAt
) {
    public ReviewResponse(Review review) {
        this(review.getId(),
            review.getRating(),
            review.getComment(),
            review.getSentiment(),
            new UserResponseDTO(review.getUser()),
            new ContactInformationDTO(review.getClient().getContactInformation()),
            review.getRoom().getCategory(),
            review.getReply(),
            review.getRepliedAt()
        );
    }
}
