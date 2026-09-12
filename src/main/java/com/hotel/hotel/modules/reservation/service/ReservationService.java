package com.hotel.hotel.modules.reservation.service;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.config.exceptions.RoomNotAvailable;
import com.hotel.hotel.modules.audit.AuditService;
import com.hotel.hotel.modules.audit.Auditable;
import com.hotel.hotel.modules.files.service.FileService;
import com.hotel.hotel.modules.notification.service.NotificationService;
import com.hotel.hotel.modules.reviews.dto.RoomRatingSummaryDTO;
import com.hotel.hotel.modules.room.repository.RoomRepository;
import com.hotel.hotel.modules.user.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import com.hotel.hotel.modules.reservation.dtos.ReservationDetailsDTO;
import com.hotel.hotel.modules.reservation.dtos.ReservationEditDTO;
import com.hotel.hotel.modules.reservation.dtos.ReservationFilters;
import com.hotel.hotel.modules.reservation.dtos.ReservationSaveDTO;
import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.reservation.model.Status;
import com.hotel.hotel.modules.reservation.repository.ReservationRepository;
import com.hotel.hotel.modules.reservation.repository.specs.ReservationSpecification;
import com.hotel.hotel.modules.room.dtos.RoomDetailsImageDTO;
import com.hotel.hotel.modules.room.model.StatusRoom;

import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ReservationService {
    
    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private UserService userService;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ReservationRepository repository;

    @Autowired
    private FileService fileService;

    @Autowired
    private NotificationService notificationService;

    @Auditable(action = "RESERVATION_CREATE", resourceType = "RESERVATION")
    @Transactional
    public ReservationDetailsDTO create(ReservationSaveDTO data) {
        log.info("Starting process to create reservation for client with ID: {}", data.userId());
        var user = userService.findById(data.userId());
        var room = roomRepository.findById(data.roomId()).orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado"));
        var files = fileService.listImagesByRoom(room.getId());
        validateReservationDate(data.checkInDate(), data.checkOutDate(), data.roomId(), null);
        BigDecimal totalAmount = calculateTotalAmount(data.checkInDate(), data.checkOutDate(), data.dailyRate(), data.serviceFee(), data.discountAmount());
        Reservation reservation = new Reservation(data);
        reservation.assignClient(user);
        reservation.assignRoom(room);
        reservation.setTotalAmount(totalAmount);
        Reservation newReservation = repository.save(reservation);
        log.info("The reservation was successfully created in the database");
        RoomRatingSummaryDTO ratingSummary = roomRepository.findRatingSummaryByRoomId(room.getId());
        ReservationDetailsDTO response = new ReservationDetailsDTO(newReservation, new RoomDetailsImageDTO(room, files, ratingSummary));
        return response;
    }

    public Page<ReservationDetailsDTO> list(ReservationFilters filters, Pageable pagination) {
        log.info("Starting process to list reservations");

        Specification<Reservation> filter = (root, query, criteriaBuilder) -> null;

        filter = filter.and(ReservationSpecification.checkDateBetween(filters.checkInDate(), filters.checkOutDate()))
                .and(ReservationSpecification.categoryIn(filters.category()))
                .and(ReservationSpecification.floorIn(filters.floor()))
                .and(ReservationSpecification.statusIn(filters.status()))
                .and(ReservationSpecification.userNameContains(filters.guestName()))
                .and(ReservationSpecification.statusIn(filters.status()));

        log.info("Returning the reservations list");

        Page<Reservation> reservations = repository.findAll(filter, pagination);

        return reservations.map(reservation -> {
            var files = fileService.listImagesByRoom(reservation.getRoom().getId());
            RoomRatingSummaryDTO ratingSummary = roomRepository.findRatingSummaryByRoomId(reservation.getRoom().getId());
            return new ReservationDetailsDTO(reservation, new RoomDetailsImageDTO(reservation.getRoom(), files, ratingSummary));
        });
    }

    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public ReservationDetailsDTO getById(Long id) {
        log.info("Finding reservation with ID: {}", id);
        var reservation = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        var files = fileService.listImagesByRoom(reservation.getRoom().getId());
        RoomRatingSummaryDTO ratingSummary = roomRepository.findRatingSummaryByRoomId(reservation.getRoom().getId());
        return new ReservationDetailsDTO(reservation, new RoomDetailsImageDTO(reservation.getRoom(), files, ratingSummary));
    }

    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public Reservation getEntityById(Long id) {
        Reservation reservation = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não existente"));
        return reservation;
    }

    @Transactional
    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public ReservationDetailsDTO edit(ReservationEditDTO data, Long id) {
        log.info("Starting process to edit reservation with ID: {}", id);
         var reservation = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        String beforeUpdate = null;
        try {
            beforeUpdate = objectMapper.writeValueAsString(reservation);
        } catch (JsonProcessingException e) {
            log.warn("Erro ao processar JSON para auditoria " + e);
        }
        validateReservationDate(data.checkInDate(), data.checkOutDate(), reservation.getRoom().getId(), reservation.getId());

        if (data.roomId() != null && !data.roomId().equals(reservation.getRoom().getId())) {
            var newRoom = roomRepository.findById(data.roomId()).orElseThrow(() -> new ResourceNotFoundException("Quarto não encontrado"));
            reservation.assignRoom(newRoom);
        }
        
        reservation.edit(data);
        log.info("Recalculating values");
        BigDecimal totalAmount = calculateTotalAmount(
                reservation.getCheckInDate(),
                reservation.getCheckOutDate(),
                reservation.getDailyRate(),
                reservation.getServiceFee(),
                reservation.getDiscountAmount());
        reservation.setTotalAmount(totalAmount);
        auditService.recordUpdate("RESERVATION_UPDATE", "RESERVATION", String.valueOf(id), beforeUpdate, reservation);
        log.info("Reservation with ID: {} was successfully edited", id);
        var files = fileService.listImagesByRoom(reservation.getRoom().getId());
        RoomRatingSummaryDTO ratingSummary = roomRepository.findRatingSummaryByRoomId(reservation.getRoom().getId());
        return new ReservationDetailsDTO(reservation, new RoomDetailsImageDTO(reservation.getRoom(), files, ratingSummary));
    }

    @Transactional
    @Auditable(action = "RESERVATION_CANCEL", resourceType = "RESERVATION")
    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public void cancel(Long id) {
        log.info("Canceling reservation with ID: {}" , id);
         var reservation = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        reservation.changeStatus(Status.CANCELED);
        log.info("Reservation with ID: {} successfully canceled" , id);
    }

    @Transactional
    @Auditable(action = "RESERVATION_CONFIRM", resourceType = "RESERVATION")
    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public void confirm(Long id) {
        log.info("Confirming reservation with ID: {}" , id);
         var reservation = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        reservation.changeStatus(Status.CONFIRMED);
        log.info("Reservation with ID: {} successfully confirmed" , id);
    }

    @Auditable(action = "RESERVATION_CHECKIN", resourceType = "RESERVATION")
    @Transactional
    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public void checkIn(Long id) {
        log.info("Checking in to the reservation with ID: {}", id);
         var reservation = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        var room = reservation.getRoom();
        room.changeStatus(StatusRoom.OCCUPIED);
        reservation.changeStatus(Status.CHECKED_IN);
    }

    @Auditable(action = "RESERVATION_CHECKOUT", resourceType = "RESERVATION")
    @Transactional
    @PreAuthorize("@securityHelper.hasClientReservationPermission(#id)")
    public void checkOut(Long id) {
        log.info("Checking out to the reservation with ID: {}", id);
         var reservation = repository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Reservation not found"));
        var room = reservation.getRoom();
        room.changeStatus(StatusRoom.CLEANING);
        notificationService.create(reservation.getUser(), reservation);
        reservation.changeStatus(Status.CHECKED_OUT);
    }

    @Auditable(action = "RESERVATION_LIST_BY_USER", resourceType = "RESERVATION")
    public Page<ReservationDetailsDTO> listReservationsByUser(Long clientId, Pageable pagination, ReservationFilters filters) {
        log.info("Listing reservations of the client with ID: {}", clientId);

        Specification<Reservation> filter = (root, query, criteriaBuilder) -> null;

        filter = filter.and(ReservationSpecification.checkDateBetween(filters.checkInDate(), filters.checkOutDate()))
                .and(ReservationSpecification.categoryIn(filters.category()))
                .and(ReservationSpecification.floorIn(filters.floor()))
                .and(ReservationSpecification.statusIn(filters.status()))
                .and(ReservationSpecification.userNameContains(filters.guestName()))
                .and(ReservationSpecification.statusIn(filters.status()));

        Page<Reservation> reservationPage = repository.findAll(filter, pagination);

        if (reservationPage.getContent().size() == 0) throw new ResourceNotFoundException("Não há reservas");
        return reservationPage.map(reservation -> {
            var files = fileService.listImagesByRoom(reservation.getRoom().getId());
            RoomRatingSummaryDTO ratingSummary = roomRepository.findRatingSummaryByRoomId(reservation.getRoom().getId());
            return new ReservationDetailsDTO(reservation, new RoomDetailsImageDTO(reservation.getRoom(), files, ratingSummary));
        });
    }

    private BigDecimal calculateTotalAmount(LocalDate checkIn, LocalDate checkOut, BigDecimal dailyRate, BigDecimal serviceFee, BigDecimal discountAmount) {
        var days = checkOut.toEpochDay() - checkIn.toEpochDay();
        return dailyRate.multiply(BigDecimal.valueOf(days)).add(serviceFee).subtract(discountAmount);
    }

    private void validateReservationDate(LocalDate checkIn, LocalDate checkOut, Long roomId, Long id) {
        if (repository.existsOverlappingById(roomId, checkIn, checkOut, id)) throw new RoomNotAvailable("Quarto não disponível na data indicada");
    }
}
