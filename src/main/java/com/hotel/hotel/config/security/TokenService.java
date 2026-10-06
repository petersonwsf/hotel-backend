package com.hotel.hotel.config.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hotel.hotel.modules.user.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String createToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                .withIssuer("hotel_api")
                .withSubject(user.getLogin())
                .withClaim("id", user.getId())
                .withClaim("name", user.getName())
                .withClaim("role", user.getRole().name())
                .withClaim("imageKey", user.getProfilePicture())
                .withExpiresAt(createExpireToken())
                .sign(algorithm);
        } catch (JWTCreationException exception) {
            // Log completo apenas no servidor
            log.error("Falha ao criar token JWT para o usuário: {}", user.getLogin(), exception);
            throw new IllegalStateException("Não foi possível gerar o token de autenticação.");
        }
    }

    /**
     * Extrai o subject (login) de um token JWT.
     * Lança {@link JWTVerificationException} para tokens inválidos/expirados —
     * o SecurityFilter é responsável por converter isso em 401.
     */
    public String getSubject(String token) {
        Algorithm algorithm = Algorithm.HMAC256(secret);
        return JWT.require(algorithm)
            .withIssuer("hotel_api")
            .build()
            .verify(token)
            .getSubject();
    }

    public Instant createExpireToken() {
        return LocalDateTime.now().plusHours(12).toInstant(ZoneOffset.of("-03:00"));
    }
}
