package com.hotel.hotel.modules.reviews.repository.specs;

import com.hotel.hotel.modules.reviews.model.Review;
import com.hotel.hotel.modules.reviews.model.Sentiment;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;

public class ReviewSpecification {

    public static Specification<Review> filterByRoom(Long id) {
        return (root, query, builder) -> {
            if (id == null) return null;
            return builder.equal(root.get("room").get("id"), id);
        };
    }

    public static Specification<Review> filterBySentiment(List<Sentiment> sentiment) {
        return (root, query, builder) -> {
            if (sentiment == null || sentiment.isEmpty()) return null;
            return root.get("sentiment").in(sentiment);
        };
    }

    public static Specification<Review> filterByUser(Long id) {
        return (root, query, builder) -> {
            if (id == null) return null;
            return builder.equal(root.get("user").get("id"), id);
        };
    }

    public static Specification<Review> filterByReplied(Boolean replied) {
        return (root, query, builder) -> {
            if (replied == null) return null;
            if (replied) {
                return root.get("reply").isNotNull();
            } else {
                return root.get("reply").isNull();
            }
        };
    }

    public static Specification<Review> filterByCreatedAt(LocalDate createdAt) {
        return (root, query, builder) -> {
            if (createdAt == null) return null;
            return builder.greaterThanOrEqualTo(root.get("createdAt"), createdAt);
        };
    }

    public static Specification<Review> filterByCommented(Boolean commented) {
        return (root, query, builder) -> {
            if (commented == null) return null;
            if (commented) {
                return root.get("comment").isNotNull();
            } else {
                return root.get("comment").isNull();
            }
        };
    }
}
