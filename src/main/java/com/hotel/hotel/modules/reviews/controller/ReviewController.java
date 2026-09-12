package com.hotel.hotel.modules.reviews.controller;

import com.hotel.hotel.modules.reviews.dto.ReviewQueryParams;
import com.hotel.hotel.modules.reviews.dto.ReviewResponse;
import com.hotel.hotel.modules.reviews.dto.ReviewSaveDTO;
import com.hotel.hotel.modules.reviews.dto.ReviewUpdateDTO;
import com.hotel.hotel.modules.reviews.model.Review;
import com.hotel.hotel.modules.reviews.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ReviewService service;

    @PostMapping
    public ResponseEntity create(@RequestBody @Valid ReviewSaveDTO reviewBody, UriComponentsBuilder uriBuilder) {
        Review review = service.createReview(reviewBody);
        var uri = uriBuilder.path("/review/{id}").buildAndExpand(review.getId()).toUri();
        return ResponseEntity.created(uri).body(new ReviewResponse(review));
    }

    @GetMapping
    public ResponseEntity list(ReviewQueryParams filters, Pageable pagination) {
        Page<ReviewResponse> reviews = service.listReview(filters, pagination).map(ReviewResponse::new);
        return ResponseEntity.ok(reviews);
    }

    @PatchMapping("/{id}")
    public ResponseEntity update(@RequestBody @Valid ReviewUpdateDTO reviewData, @PathVariable Long id) {
        Review review = service.update(reviewData, id);
        return ResponseEntity.ok(new ReviewResponse(review));
    }

    @PatchMapping("/reply/{id}")
    public ResponseEntity updateReply(@RequestBody @Valid ReviewUpdateDTO reviewData, @PathVariable Long id) {
        Review review = service.replyComment(reviewData, id);
        return ResponseEntity.ok(new ReviewResponse(review));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity delete(Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
