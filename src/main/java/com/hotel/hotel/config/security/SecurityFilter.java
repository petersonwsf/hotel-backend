package com.hotel.hotel.config.security;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import com.hotel.hotel.modules.user.repository.UserRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Map;

/**
 * Filtro de autenticação JWT.
 * <p>
 * Tokens inválidos/expirados são capturados AQUI (filtro não passa pela
 * cadeia de @ControllerAdvice) e a resposta segue o mesmo contrato
 * {@code {"message": "..."}} do RequestExceptionHandler.
 */
@Slf4j
@Component
public class SecurityFilter extends OncePerRequestFilter {

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var token = getToken(request);
        if (token != null) {
            try {
                var subject = tokenService.getSubject(token);
                var user = userRepository.findByLogin(subject);
                if (user != null) {
                    var authorization = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
                    SecurityContextHolder.getContext().setAuthentication(authorization);
                }
            } catch (JWTVerificationException ex) {
                log.warn("Token JWT inválido ou expirado | path={} | causa={}", request.getRequestURI(), ex.getMessage());
                sendErrorResponse(response, HttpStatus.UNAUTHORIZED,
                        "Token de autenticação inválido ou expirado. Faça login novamente.");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    public String getToken(HttpServletRequest httpServletRequest) {
        var token = httpServletRequest.getHeader("Authorization");
        if (token == null || token.isBlank()) {
            return null;
        }
        String tokenLimpo = token.replace("Bearer ", "").trim();
        if (tokenLimpo.equalsIgnoreCase("null") || tokenLimpo.equalsIgnoreCase("undefined") || tokenLimpo.isBlank()) {
            return null;
        }
        return tokenLimpo;
    }

    /**
     * Escreve uma resposta JSON padronizada no mesmo formato que o
     * RequestExceptionHandler ({@code {"message": "..."}}).
     */
    private void sendErrorResponse(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        String body = objectMapper.writeValueAsString(Map.of("message", message));
        response.getWriter().write(body);
    }
}
