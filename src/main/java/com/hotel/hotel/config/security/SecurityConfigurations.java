package com.hotel.hotel.config.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class SecurityConfigurations {

    @Autowired
    private SecurityFilter securityFilter;

    @Autowired
    private ObjectMapper objectMapper;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity httpSecurity) throws Exception {
        return httpSecurity
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        //ROTAS PÚBLICAS
                        .requestMatchers(HttpMethod.POST, "/user/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/client").permitAll()
                        .requestMatchers(HttpMethod.GET, "/room", "/room/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/file/room/{id}", "/file/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/room/disponibility/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/review").permitAll()
                        // ROTAS PRIVADAS

                        // CLIENTES
                        .requestMatchers(HttpMethod.GET, "/client").hasAnyAuthority("ROLE_ATTENDANT", "ROLE_ADMIN")
                        // ROOM
                        .requestMatchers(HttpMethod.POST, "/room").hasAuthority( "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/room/{id}").hasAuthority( "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/room/{id}").hasAuthority( "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/room/finishCleaning/{id}").hasAnyAuthority( "ROLE_ATTENDANT","ROLE_ADMIN")
                        // RESERVATION
                        .requestMatchers(HttpMethod.PATCH, "/reservation/checkIn/{id}", "/reservation/checkOut/{id}").hasAnyAuthority("ROLE_ATTENDANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.GET, "/reservation").hasAnyAuthority("ROLE_ATTENDANT", "ROLE_ADMIN")
                        // FILE
                        .requestMatchers(HttpMethod.DELETE, "/file/{id}").hasAnyAuthority("ROLE_ATTENDANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/user/register").hasAuthority("ROLE_ADMIN")
                        // GET
                        .requestMatchers(HttpMethod.GET, "/user").hasAuthority("ROLE_ADMIN")
                        // REVIEW
                        .requestMatchers(HttpMethod.PATCH, "/review/reply").hasAnyAuthority("ROLE_ATTENDANT", "ROLE_ADMIN")
                        .requestMatchers(HttpMethod.POST, "/review").hasAuthority("ROLE_CLIENT")
                        .requestMatchers(HttpMethod.PATCH, "/review/{id}").hasAuthority("ROLE_CLIENT")
                        .requestMatchers(HttpMethod.DELETE, "/review/{id}").hasAuthority("ROLE_CLIENT")
                        // NOTIFICATIONS
                        .requestMatchers(HttpMethod.GET, "/notification/${id}").hasAuthority("ROLE_CLIENT")
                        .requestMatchers(HttpMethod.PATCH, "/notification/{id}").hasAuthority("ROLE_CLIENT")
                        .anyRequest().authenticated()
                ).exceptionHandling(ex -> ex
                        // 401 – usuário não autenticado (sem token ou token rejeitado pelo filtro)
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpStatus.UNAUTHORIZED.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String body = objectMapper.writeValueAsString(
                                    Map.of("message", "Usuário não autenticado. Faça login para continuar."));
                            response.getWriter().write(body);
                        })
                        // 403 – autenticado, mas sem permissão suficiente
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                            response.setCharacterEncoding("UTF-8");
                            String body = objectMapper.writeValueAsString(
                                    Map.of("message", "Você não tem permissão para acessar este recurso."));
                            response.getWriter().write(body);
                        })
                )
                .addFilterBefore(securityFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager getManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
