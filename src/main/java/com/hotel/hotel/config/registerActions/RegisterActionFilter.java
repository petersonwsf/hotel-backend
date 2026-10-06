package com.hotel.hotel.config.registerActions;

import java.io.IOException;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hotel.hotel.config.security.TokenService;
import com.hotel.hotel.modules.user.model.User;
import com.hotel.hotel.modules.user.repository.UserRepository;
import com.hotel.hotel.modules.userActions.model.Action;
import com.hotel.hotel.modules.userActions.model.UserAction;
import com.hotel.hotel.modules.userActions.repository.UserActionRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

/**
 * Registra ações do usuário (CREATE_RESERVATION, etc.) de forma
 * best-effort: erros NUNCA interrompem o fluxo principal.
 */
@Slf4j
@Component
public class RegisterActionFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserActionRepository actionRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var action = getActionFromRequest(request);

        if (action != null) {
            var token = getToken(request);
            if (token != null) {
                try {
                    var subject = tokenService.getSubject(token);
                    User user = userRepository.findByUsername(subject);
                    if (user != null) {
                        log.info("Register action: {} by user: {}", action.name(), user.getId());
                        var actionSaved = new UserAction(action, user);
                        actionRepository.save(actionSaved);
                    }
                } catch (JWTVerificationException ex) {
                    // Token inválido: registro de ação é best-effort, não interrompe a requisição
                    log.debug("Não foi possível registrar ação — token inválido | action={}", action.name());
                } catch (Exception ex) {
                    // Falha de registro nunca deve quebrar a requisição principal
                    log.warn("Falha ao registrar ação de usuário | action={}", action.name(), ex);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    public String getToken(HttpServletRequest httpServletRequest) {
        var token = httpServletRequest.getHeader("Authorization");
        if (token != null) {
            return token.replace("Bearer ", "").trim();
        }
        return null;
    }

    public Action getActionFromRequest(HttpServletRequest request) {
        var method = request.getMethod();
        var uri = request.getRequestURI();

        if (method.equals("POST") && uri.matches("^/reservation/?$")) return Action.CREATE_RESERVATION;
        if (method.equals("PATCH") && uri.matches("^/reservation/[^/]+$")) return Action.EDIT_RESERVATION;
        if (method.equals("PATCH") && uri.matches("^/reservation/confirm/[^/]+$")) return Action.CONFIRM_RESERVATION;
        if (method.equals("PATCH") && uri.matches("^/reservation/checkIn/[^/]+$")) return Action.CHECK_IN;
        if (method.equals("PATCH") && uri.matches("^/reservation/checkOut/[^/]+$")) return Action.CHECK_OUT;
        if (method.equals("DELETE") && uri.matches("^/reservation/[^/]+$")) return Action.CANCEL_RESERVATION;

        return null;
    }
}
