package com.hotel.hotel.modules.reservation.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.reservation.model.Status;

public interface ReservationRepository extends JpaRepository<Reservation, Long>, JpaSpecificationExecutor<Reservation> {
    Page<Reservation> findByStatusNot(Pageable pageable, Status status);
    Page<Reservation> findByUserId(Pageable pageable, Long clientId);
    Page<Reservation> findByUserId(Long clientId, Pageable pageable);


    @Query("SELECT COUNT(r) > 0 FROM Reservation r " +
            "WHERE r.room.id = :roomId " +
            "AND r.status <> 'CANCELLED' " +
            "AND r.checkInDate < :endDate " +
            "AND r.checkOutDate > :startDate " +
            "AND (:id IS NULL OR r.id <> :id)")
    boolean existsOverlappingById(
            @Param("roomId") Long roomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("id") Long id
    );
}
