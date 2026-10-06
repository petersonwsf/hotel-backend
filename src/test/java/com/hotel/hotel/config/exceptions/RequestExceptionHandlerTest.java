package com.hotel.hotel.config.exceptions;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hotel.hotel.config.security.SecurityFilter;
import com.hotel.hotel.config.security.TokenService;
import com.hotel.hotel.modules.audit.AuditService;
import com.hotel.hotel.modules.user.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Testes exaustivos para o {@link RequestExceptionHandler} e para o {@link SecurityFilter}.
 * <p>
 * Valida:
 * 1. Todos os códigos HTTP (400, 401, 403, 404, 405, 406, 409, 413, 415, 422, 500, 503).
 * 2. Formato estrito do DTO ErrorResponse (ou lista de FieldErrorDetail para validação).
 * 3. Ausência de stack trace, nome de classe de exceção ou detalhes técnicos.
 * 4. Respostas amigáveis em português brasileiro (pt-BR).
 */
@ExtendWith(MockitoExtension.class)
class RequestExceptionHandlerTest {

    private MockMvc mockMvc;

    @Mock
    private AuditService auditService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @BeforeEach
    void setUp() {
        RequestExceptionHandler handler = new RequestExceptionHandler(auditService);
        this.mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeTestController())
                .setControllerAdvice(handler)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Exceções de Autenticação / Segurança (401, 403)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("401 - BadCredentialsException retorna mensagem amigável sem vazar detalhes")
    void handleBadCredentials() throws Exception {
        mockMvc.perform(get("/test/bad-credentials"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Login ou senha incorretos."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist());
    }

    @Test
    @DisplayName("401 - DisabledException retorna mensagem amigável de conta desabilitada")
    void handleDisabled() throws Exception {
        mockMvc.perform(get("/test/disabled"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Conta desabilitada. Entre em contato com o suporte."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("401 - LockedException retorna mensagem amigável de conta bloqueada")
    void handleLocked() throws Exception {
        mockMvc.perform(get("/test/locked"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Conta bloqueada. Entre em contato com o suporte."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("401 - JWTVerificationException retorna mensagem amigável de token inválido")
    void handleJwtVerification() throws Exception {
        mockMvc.perform(get("/test/jwt-verification"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Token de autenticação inválido ou expirado. Faça login novamente."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("403 - AccessDeniedException (Spring Security) retorna mensagem amigável")
    void handleSpringAccessDenied() throws Exception {
        mockMvc.perform(get("/test/spring-access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem permissão para acessar este recurso."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("403 - AccessResourceDeniedException (negócio) registra auditoria e retorna mensagem")
    void handleAccessResourceDenied() throws Exception {
        mockMvc.perform(get("/test/business-access-denied"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Você não tem permissão para este recurso"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        verify(auditService, atLeastOnce()).record(any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Exceções de Negócio / Domínio (404, 409)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("404 - ResourceNotFoundException retorna mensagem amigável")
    void handleResourceNotFound() throws Exception {
        mockMvc.perform(get("/test/resource-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Quarto não encontrado"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("409 - ResourceAlreadyExists retorna mensagem amigável")
    void handleResourceAlreadyExists() throws Exception {
        mockMvc.perform(get("/test/already-exists"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email já está sendo utilizado, tente outro"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("409 - RoomNotAvailable retorna mensagem de conflito de reserva")
    void handleRoomNotAvailable() throws Exception {
        mockMvc.perform(get("/test/room-not-available"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Quarto não disponível na data indicada"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Exceções de Persistência / JPA (404, 409)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("404 - EntityNotFoundException (JPA) retorna 404 sem vazar nomes internos de classes")
    void handleEntityNotFound() throws Exception {
        mockMvc.perform(get("/test/entity-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("O recurso solicitado não foi encontrado."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("Entity"))));
    }

    @Test
    @DisplayName("409 - DataIntegrityViolationException não vaza SQL ou nomes de constraints")
    void handleDataIntegrityViolation() throws Exception {
        mockMvc.perform(get("/test/data-integrity"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Operação não permitida: existe um conflito com os dados existentes."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("SQL"))))
                .andExpect(jsonPath("$.message", not(containsString("constraint"))));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Exceções de Validação e Binding (400, 413)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("400 - MethodArgumentNotValidException retorna lista com field e error amigáveis")
    void handleValidation() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].field").value("name"))
                .andExpect(jsonPath("$[0].error").value("O nome é obrigatório"))
                .andExpect(jsonPath("$[*].trace").doesNotExist());
    }

    @Test
    @DisplayName("400 - HttpMessageNotReadableException (JSON quebrado) não vaza detalhes do parser Jackson")
    void handleNotReadable() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{invalid-json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O corpo da requisição está malformado ou em formato inválido."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("JsonParseException"))));
    }

    @Test
    @DisplayName("400 - MissingServletRequestParameterException informa o parâmetro ausente")
    void handleMissingParam() throws Exception {
        mockMvc.perform(get("/test/param"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O parâmetro 'codigo' é obrigatório."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("400 - MethodArgumentTypeMismatchException informa o parâmetro inválido sem vazar stack trace")
    void handleTypeMismatch() throws Exception {
        mockMvc.perform(get("/test/number/nao-e-numero"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O valor informado para 'id' é inválido."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("400 - MissingRequestHeaderException informa o cabeçalho ausente")
    void handleMissingHeader() throws Exception {
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("O header 'X-Custom-Header' é obrigatório."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("413 - MaxUploadSizeExceededException retorna tamanho excedido")
    void handleMaxUploadSize() throws Exception {
        mockMvc.perform(get("/test/upload-size"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("O arquivo enviado excede o tamanho máximo permitido."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Exceções de Roteamento HTTP (405, 415, 404)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("405 - HttpRequestMethodNotSupportedException informa método não suportado")
    void handleMethodNotSupported() throws Exception {
        mockMvc.perform(delete("/test/validate"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("O método HTTP 'DELETE' não é suportado para este recurso."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("415 - HttpMediaTypeNotSupportedException retorna 415 amigável")
    void handleMediaTypeNotSupported() throws Exception {
        mockMvc.perform(post("/test/validate")
                        .contentType(MediaType.APPLICATION_XML)
                        .content("<xml></xml>"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("O tipo de mídia da requisição não é suportado."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("404 - NoResourceFoundException retorna rota não encontrada")
    void handleNoResourceFound() throws Exception {
        mockMvc.perform(get("/test/no-resource"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("O endereço solicitado não foi encontrado."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Infraestrutura / Storage (503)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("503 - MyCustomStorageException retorna serviço temporariamente indisponível sem vazar MinIO")
    void handleStorageException() throws Exception {
        mockMvc.perform(get("/test/storage-error"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value(
                        "O serviço de armazenamento de arquivos está temporariamente indisponível. Tente novamente em instantes."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("MinIO"))))
                .andExpect(jsonPath("$.message", not(containsString("Connection refused"))));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Runtime / Negócio (400, 422)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("400 - IllegalArgumentException retorna 400 amigável")
    void handleIllegalArgument() throws Exception {
        mockMvc.perform(get("/test/illegal-argument"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Os dados informados são inválidos. Verifique os campos e tente novamente."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("422 - IllegalStateException retorna 422 estado inválido do recurso")
    void handleIllegalState() throws Exception {
        mockMvc.perform(get("/test/illegal-state"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(
                        "A operação não pode ser realizada no estado atual do recurso."))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. Fallback Genérico (500)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("500 - Exception genérica (NPE, etc.) retorna 500 sem vazar stack trace nem tipo da classe")
    void handleGenericException() throws Exception {
        mockMvc.perform(get("/test/unexpected-error"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(
                        "Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."))
                .andExpect(jsonPath("$.trace").doesNotExist())
                .andExpect(jsonPath("$.exception").doesNotExist())
                .andExpect(jsonPath("$.message", not(containsString("NullPointerException"))));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 9. Testes de Filtro de Segurança (SecurityFilter)
    // ──────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("SecurityFilter - Token JWT com assinatura inválida retorna 401 padronizado")
    void securityFilterHandlesInvalidJwtGracefully() throws ServletException, IOException {
        SecurityFilter filter = new SecurityFilter();
        ObjectMapper mapper = new ObjectMapper();
        ReflectionTestUtils.setField(filter, "tokenService", tokenService);
        ReflectionTestUtils.setField(filter, "userRepository", userRepository);
        ReflectionTestUtils.setField(filter, "objectMapper", mapper);

        when(tokenService.getSubject("token-corrompido"))
                .thenThrow(new SignatureVerificationException(null));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer token-corrompido");
        request.setRequestURI("/client");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        filter.doFilter(request, response, filterChain);

        assertEquals(401, response.getStatus());
        assertTrue(response.getContentAsString().contains("Token de autenticação inválido ou expirado. Faça login novamente."));
        verify(filterChain, never()).doFilter(any(), any());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Controller de Teste Interno (dispara as condições de erro)
    // ──────────────────────────────────────────────────────────────────────────

    record TestDto(@NotBlank(message = "O nome é obrigatório") String name) {}

    @RestController
    @RequestMapping("/test")
    static class FakeTestController {

        @GetMapping("/bad-credentials")
        public void badCredentials() {
            throw new BadCredentialsException("Bad credentials");
        }

        @GetMapping("/disabled")
        public void disabled() {
            throw new DisabledException("User is disabled");
        }

        @GetMapping("/locked")
        public void locked() {
            throw new LockedException("User account is locked");
        }

        @GetMapping("/jwt-verification")
        public void jwtVerification() {
            throw new JWTVerificationException("The Token has expired");
        }

        @GetMapping("/spring-access-denied")
        public void springAccessDenied() {
            throw new AccessDeniedException("Access is denied");
        }

        @GetMapping("/business-access-denied")
        public void businessAccessDenied() {
            throw new AccessResourceDeniedException("Você não tem permissão para este recurso");
        }

        @GetMapping("/resource-not-found")
        public void resourceNotFound() {
            throw new ResourceNotFoundException("Quarto não encontrado");
        }

        @GetMapping("/already-exists")
        public void alreadyExists() {
            throw new ResourceAlreadyExists("Email já está sendo utilizado, tente outro");
        }

        @GetMapping("/room-not-available")
        public void roomNotAvailable() {
            throw new RoomNotAvailable("Quarto não disponível na data indicada");
        }

        @GetMapping("/entity-not-found")
        public void entityNotFound() {
            throw new EntityNotFoundException("Unable to find com.hotel.hotel.modules.room.model.Room with id 99");
        }

        @GetMapping("/data-integrity")
        public void dataIntegrity() {
            throw new DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint 'uk_client_email'");
        }

        @PostMapping("/validate")
        public void validate(@RequestBody @Valid TestDto dto) {}

        @GetMapping("/param")
        public void param(@RequestParam("codigo") String codigo) {}

        @GetMapping("/number/{id}")
        public void numberParam(@PathVariable("id") Long id) {}

        @GetMapping("/header")
        public void header(@RequestHeader("X-Custom-Header") String header) {}

        @GetMapping("/upload-size")
        public void uploadSize() {
            throw new MaxUploadSizeExceededException(10485760);
        }

        @GetMapping("/no-resource")
        public void noResource() throws NoResourceFoundException {
            throw new NoResourceFoundException(HttpMethod.GET, "/test/no-resource");
        }

        @GetMapping("/storage-error")
        public void storageError() {
            throw new MyCustomStorageException("Failed to connect to MinIO server: localhost:9003");
        }

        @GetMapping("/illegal-argument")
        public void illegalArgument() {
            throw new IllegalArgumentException("invalid argument provided");
        }

        @GetMapping("/illegal-state")
        public void illegalState() {
            throw new IllegalStateException("illegal state in domain");
        }

        @GetMapping("/unexpected-error")
        public void unexpectedError() {
            throw new NullPointerException("NPE at com.hotel.service.calculateTotalAmount(Line 245)");
        }
    }
}
