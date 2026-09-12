package com.hotel.hotel.modules.reviews.model;

import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.room.model.Room;
import com.hotel.hotel.modules.user.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity(name = "Review")
@Table(name = "review")
@AllArgsConstructor
@NoArgsConstructor
public class Review {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false, unique = true)
    private Reservation reservation;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;
    @Column(nullable = false)
    private Double rating;
    @Enumerated(EnumType.STRING)
    private Sentiment sentiment;
    @Column(columnDefinition = "TEXT")
    private String comment;
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    private String reply;
    @Column(name = "replied_at")
    private LocalDateTime repliedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
