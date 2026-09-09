package com.hotel.hotel.modules.room.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.hotel.config.exceptions.ResourceAlreadyExists;
import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.modules.audit.AuditService;
import com.hotel.hotel.modules.audit.Auditable;
import com.hotel.hotel.modules.files.service.FileService;
import com.hotel.hotel.modules.room.dtos.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import com.hotel.hotel.modules.room.model.Room;
import com.hotel.hotel.modules.room.model.StatusRoom;
import com.hotel.hotel.modules.room.repository.RoomRepository;
import com.hotel.hotel.modules.room.repository.specs.RoomSpecification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class RoomService {

    @Autowired
    private RoomRepository repository;
    @Autowired
    private FileService fileService;
    @Autowired
    private AuditService auditService;
    @Autowired
    private ObjectMapper objectMapper;

    @Auditable(action = "ROOM_CREATE", resourceType = "ROOM")
    @Transactional
    public Room create(RoomSaveDTO data, List<MultipartFile> files) {
        log.info("Starting process to create room");
        var roomCodeAlreadyExists = repository.findByCode(data.code());

        if (roomCodeAlreadyExists.isPresent()) {
            throw new ResourceAlreadyExists("Room code already exists");
        }

        var room = new Room(data);

        var newRoom = repository.save(room);
        log.info("Room successfully created in the database");

        for (MultipartFile file : files) {
            fileService.uploadFile(file, newRoom, null);
        }

        return newRoom;
    }

    public Page<RoomListDTO> list(RoomFiltersDTO filters, Pageable pagination) {
        log.info("Starting listing rooms");
        Specification<Room> filter = (root, query, criteriaBuilder) -> null;

        filter = filter.and(RoomSpecification.codeEqual(filters.code()))
                .and(RoomSpecification.roomAvailableOn(filters.checkInDate(), filters.checkOutDate(), null))
                .and(RoomSpecification.activeEqual(filters.active()))
                .and(RoomSpecification.floorIn(filters.floor()))
                .and(RoomSpecification.statusIn(filters.status()))
                .and(RoomSpecification.priceBetween(filters.minPrice(), filters.maxPrice()))
                .and(RoomSpecification.capacityGreaterThan(filters.capacity()))
                .and(RoomSpecification.categoryIn(filters.category()));
        log.info("Returning rooms list");

        Page<Room> rooms = repository.findAll(filter, pagination);

        return rooms.map(room -> {
            List<String> files = fileService.listImagesByRoom(room.getId());
            String image = files.isEmpty() ? null : files.getFirst();
            return new RoomListDTO(room, image);
        });
    }

    public RoomDetailsImageDTO getDetails(Long id) {
        log.info("Starting process to retrieve details of the room with ID: {}", id);
        var room = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        List<String> images = fileService.listImagesByRoom(room.getId());

        log.info("Returning details of the room with ID: {}", id);
        return new RoomDetailsImageDTO(room, images);
    }

    @Transactional
    public Room edit(RoomEditDTO data, Long id, List<MultipartFile> newImages) {
        log.info("Starting process to edit room with ID: {}", id);
        var room = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        String beforeUpdate = null;
        beforeUpdate = serializeForAudit(room);

        if (data.remainingImages() != null && !data.remainingImages().isEmpty()) {
            fileService.syncRoomImages(id, data.remainingImages(), newImages, room);
        }
        
        room.edit(data);
        auditService.recordUpdate("ROOM_UPDATE", "ROOM", String.valueOf(id), beforeUpdate, room);
        log.info("Room with ID: {} successfully edited", id);
        return room;
    }

    @Auditable(action = "FINISH_CLEANING_ROOM", resourceType = "ROOM")
    @Transactional
    public void finishCleaning(Long id) {
        log.info("Starting process to clean room with ID: {}", id);
        var room = repository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado"));
        room.changeStatus(StatusRoom.AVAILABLE);
        log.info("Room with ID: {} successfully cleaned", id);
    }

    @Auditable(action = "ROOM_DELETE", resourceType = "ROOM")
    @Transactional
    public void delete(Long id) {
        log.info("Starting process to delete room with ID: {}", id);
        var room = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Room not found"));
        repository.delete(room);
        log.info("Room with ID: {} successfully deleted", id);
    }

    @Auditable(action = "VERIFY_DISPONIBILITY", resourceType = "ROOM")
    public Boolean verifyDisponibility(Long id, VerifyDisponibilityDTO verifyData) {
        Specification<Room> specId = (root, query, cb) -> cb.equal(root.get("id"), id);
        Specification<Room> specFinal = specId.and(RoomSpecification.roomAvailableOn(verifyData.checkIn(), verifyData.checkOut(), verifyData.reservationId()));
        Optional<Room> room = repository.findOne(specFinal);
        return room.isPresent();
    }

    private String serializeForAudit(Room room) {
    try {
        return objectMapper.writeValueAsString(room);
    } catch (JsonProcessingException e) {
        log.warn("Erro ao processar JSON para auditoria: {}", e.getMessage());
        return null;
    }
}
}
