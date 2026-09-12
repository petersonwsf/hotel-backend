package com.hotel.hotel.modules.reviews.service;

import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.modules.audit.Auditable;
import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.client.service.ClientService;
import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.reservation.service.ReservationService;
import com.hotel.hotel.modules.reviews.dto.ReviewQueryParams;
import com.hotel.hotel.modules.reviews.dto.ReviewSaveDTO;
import com.hotel.hotel.modules.reviews.dto.ReviewUpdateDTO;
import com.hotel.hotel.modules.reviews.model.Review;
import com.hotel.hotel.modules.reviews.model.Sentiment;
import com.hotel.hotel.modules.reviews.repository.ReviewRepository;
import com.hotel.hotel.modules.reviews.repository.specs.ReviewSpecification;
import com.hotel.hotel.modules.user.model.User;
import com.hotel.hotel.modules.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.AccessDeniedException;
import java.time.LocalDateTime;

@Service
@Slf4j
public class ReviewService {

    @Autowired
    private ReviewRepository repository;
    @Autowired
    private ReservationService reservationService;
    @Autowired
    private UserService userService;
    @Autowired
    private ClientService clientService;

    @Transactional
    @Auditable(action = "REVIEW_CREATED", resourceType = "REVIEW")
    @PreAuthorize("@securityHelper.hasUserPermissionReviewCreate(#reviewBody)")
    public Review createReview(ReviewSaveDTO reviewBody) {
        Reservation reservation = reservationService.getEntityById(reviewBody.reservationId());
        log.info("Iniciando criação de avaliação. ReservationId: {}, UserId: {}", reservation.getId(), reservation.getUser().getId());
        Client client = clientService.getClientByUserId(reservation.getUser().getId());
        Review reviewData = new Review();
        Sentiment sentiment = determineSentimentFromRating(reviewBody.rating());
        reviewData.setUser(reservation.getUser());
        reviewData.setReservation(reservation);
        reviewData.setClient(client);
        reviewData.setSentiment(sentiment);
        reviewData.setRoom(reservation.getRoom());
        reviewData.setComment(reviewBody.comment());
        reviewData.setRating(reviewBody.rating());
        return repository.save(reviewData);
    }

    public Page<Review> listReview(ReviewQueryParams filters, Pageable pagination) {
        Specification<Review> filter = (root, query, builder) -> null;
        filter = filter.and(ReviewSpecification.filterByUser(filters.userId()))
                .and(ReviewSpecification.filterByRoom(filters.roomId()))
                .and(ReviewSpecification.filterByReplied(filters.replied()))
                .and(ReviewSpecification.filterBySentiment(filters.sentiment()))
                .and(ReviewSpecification.filterByCommented(filters.commented()))
                .and(ReviewSpecification.filterByCreatedAt(filters.createdAt()));

        return repository.findAll(filter, pagination);
    }

    public Review findReviewById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Avaliação não encontrada"));
    }

    @Transactional
    @PreAuthorize("@securityHelper.hasUserPermissionReview(#id)")
    @Auditable(action = "REVIEW_UPDATE", resourceType = "REVIEW")
    public Review update(ReviewUpdateDTO reviewData, Long id) {
        Review review = findReviewById(id);
        review.setRating(reviewData.rating());
        review.setComment(reviewData.comment());
        return review;
    }

    @Transactional
    @Auditable(action = "REVIEW_CREATED", resourceType = "REVIEW")
    public Review replyComment(ReviewUpdateDTO reviewData, Long id) {
        Review review = findReviewById(id);
        review.setReply(reviewData.reply());
        review.setRepliedAt(LocalDateTime.now());
        return review;
    }

    @Transactional
    @PreAuthorize("@securityHelper.hasUserPermissionReview(#id)")
    @Auditable(action = "REVIEW_DELETED", resourceType = "REVIEW")
    public void delete(Long id) {
        repository.deleteById(id);
    }

    private static Sentiment determineSentimentFromRating(Double rating) {
        if (rating < 3) {
            return Sentiment.NEGATIVE;
        } else if (rating == 3) {
            return Sentiment.NEUTRAL;
        } else {
            return Sentiment.POSITIVE;
        }
    }
}
