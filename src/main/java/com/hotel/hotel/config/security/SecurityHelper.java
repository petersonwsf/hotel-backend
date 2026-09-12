package com.hotel.hotel.config.security;

import com.hotel.hotel.modules.client.repository.ClientRepository;
import com.hotel.hotel.modules.notification.model.Notification;
import com.hotel.hotel.modules.notification.repository.NotificationRepository;
import com.hotel.hotel.modules.reviews.dto.ReviewSaveDTO;
import com.hotel.hotel.modules.reviews.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.hotel.hotel.config.exceptions.AccessResourceDeniedException;
import com.hotel.hotel.config.exceptions.ResourceNotFoundException;
import com.hotel.hotel.modules.client.model.Client;
import com.hotel.hotel.modules.reservation.model.Reservation;
import com.hotel.hotel.modules.reservation.repository.ReservationRepository;
import com.hotel.hotel.modules.user.model.Role;
import com.hotel.hotel.modules.user.model.User;

@Component("securityHelper")
public class SecurityHelper {

    @Autowired
    private ReservationRepository reservationRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private NotificationRepository notificationRepository;
    
    public boolean hasClientPermission(Long id) {
        User user = getAuthenticatedUser();
        Client client = clientRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cliente não encontrado"));
        if (!client.getUser().getId().equals(user.getId()) && user.getRole() == Role.CLIENT) throw new AccessResourceDeniedException("Você não tem acesso a este recurso");
        return true;
    }

    public boolean hasUserPermissionReviewCreate(ReviewSaveDTO review) {
        User user = getAuthenticatedUser();
        var reservation = reservationRepository.findById(review.reservationId())
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
        if (!reservation.getUser().getId().equals(user.getId()) && user.getRole() == Role.CLIENT) throw new AccessResourceDeniedException("Você não tem acesso a este recurso");
        return true;
    }

    public boolean hasUserPermissionReview(Long id) {
        User user = getAuthenticatedUser();
        var review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
        if (!review.getUser().getId().equals(user.getId())) throw new AccessResourceDeniedException("Você não tem acesso a este recurso");
        return true;
    }

    public boolean hasClientUserPermission(Long id) {
        User user = getAuthenticatedUser();
        if (!user.getId().equals(user.getId()) && user.getRole() == Role.CLIENT) throw new AccessResourceDeniedException("Você não tem acesso a este recurso");
        return true;
    }

    public boolean hasUserPermission(Long id) {
        User userAuthenticated = getAuthenticatedUser();
        if ((!userAuthenticated.getId().equals(id)) && userAuthenticated.getRole() != Role.ADMIN) throw new AccessResourceDeniedException("Você não tem permissão para este recurso");
        return true;
    }

    public boolean hasClientReservationPermission(Long id) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true;
        }
        if (!(auth.getPrincipal() instanceof User user)) {
            return true;
        }
        var reservation = reservationRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Reserva não encontrada"));
        if ((reservation.getUser().getId() != user.getId()) && user.getRole() == Role.CLIENT) throw new AccessResourceDeniedException("Você não tem acesso a este recurso");
        return true;
    }

    public boolean hasUserNotificationPermission(Long id) {
        User user = getAuthenticatedUser();
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notificação não encontrada"));
        if (!notification.getUser().getId().equals(user.getId())) throw new AccessResourceDeniedException("Você não permissão para acessar este recurso");
        return true;
    }

    private User getAuthenticatedUser() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            throw new AccessResourceDeniedException("Usuário não autenticado");
        }
        return user;
    }
}
