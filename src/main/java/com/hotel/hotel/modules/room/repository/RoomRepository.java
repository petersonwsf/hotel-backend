package com.hotel.hotel.modules.room.repository;

import java.util.Optional;

import com.hotel.hotel.modules.reviews.dto.RoomRatingSummaryDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.hotel.hotel.modules.room.model.Room;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long>, JpaSpecificationExecutor<Room> {
    Optional<Room> findByCode(String code);
    @Query("SELECT new com.hotel.hotel.modules.reviews.dto.RoomRatingSummaryDTO(" +
            "COALESCE(AVG(r.rating), 0.0), COUNT(r)) " +
            "FROM Review r WHERE r.room.id = :roomId")
    RoomRatingSummaryDTO findRatingSummaryByRoomId(@Param("roomId") Long roomId);
}
