package com.hotel.hotel.modules.room.model;

import java.math.BigDecimal;

import com.hotel.hotel.modules.room.dtos.RoomEditDTO;
import com.hotel.hotel.modules.room.dtos.RoomSaveDTO;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Table(name = "room")
@Entity(name = "Room")
@Builder
public class Room {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String code;
    private String floor;
    private BigDecimal customPrice;
    @Column(name = "amenities", columnDefinition = "text[]")
    @Builder.Default
    private String[] amenities = new String[0];
    private Integer capacity;

    @Enumerated(EnumType.STRING)
    private Category category;

    @Column(nullable = false)
    private Boolean active;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    private StatusRoom status;

    public Room(RoomSaveDTO data) {
        this.code = data.code();
        this.floor = data.floor();
        this.customPrice = data.customPrice();
        this.status = data.status();
        this.amenities = data.amenities().toArray(new String[0]);
        this.capacity = data.capacity();
        this.category = data.category();
        this.description = data.description();
        this.active = true;
    }

    public void edit(RoomEditDTO data) {
        if (data.customPrice() != null) {
            this.customPrice = data.customPrice();
        }
        if (data.amenities() != null) {
            this.amenities = data.amenities().toArray(new String[0]);
        }
        if (data.capacity() != null) {
            this.capacity = data.capacity();
        }
        if (data.category() != null) {
            this.category = data.category();
        }
        if (data.description() != null) {
            this.description = data.description();
        }
    }

    public void changeStatus(StatusRoom status) {
        this.status = status;
    }

    public void changeActive() {
        this.active = !this.active;
    }
}
