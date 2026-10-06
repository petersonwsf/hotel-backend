package com.hotel.hotel.config.exceptions;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.hotel.hotel.modules.audit.AuditEvent;
import com.hotel.hotel.modules.audit.AuditOutcome;
import com.hotel.hotel.modules.audit.AuditService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Optional;

/**
 * Ponto central de tratamento de exceções HTTP.
 * <p>
 * CONTRATO: toda resposta de erro retorna {@link ErrorResponse} ou
 * {@code List<FieldErrorDetail>} (para erros de validação de campo).
 * Nenhum stack trace, mensagem técnica ou SQL é exposto ao cliente.
 */
@Slf4j
@RestControllerAdvice
public class RequestExceptionHandler {

    // ──────────────────────────────────────────────────────────────────────────
    // Infraestrutura interna
    // ──────────────────────────────────────────────────────────────────────────

    @Autowired(required = false)
    private AuditService auditService;

    public RequestExceptionHandler() {}

    public RequestExceptionHandler(AuditService auditService) {
        this.auditService = auditService;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 1. Exceções de autenticação / segurança
    // ──────────────────────────────────────────────────────────────────────────

    /** 401 – credenciais inválidas (login/senha errados). */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Falha de autenticação | path={} method={}", request.getRequestURI(), request.getMethod());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Login ou senha incorretos."));
    }

    /** 401 – conta desabilitada. */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleDisabled(
            DisabledException ex, HttpServletRequest request) {
        log.warn("Tentativa de acesso com conta desabilitada | path={}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Conta desabilitada. Entre em contato com o suporte."));
    }

    /** 401 – conta bloqueada. */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleLocked(
            LockedException ex, HttpServletRequest request) {
        log.warn("Tentativa de acesso com conta bloqueada | path={}", request.getRequestURI());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Conta bloqueada. Entre em contato com o suporte."));
    }

    /** 401 – token JWT inválido, expirado ou com assinatura corrompida. */
    @ExceptionHandler(JWTVerificationException.class)
    public ResponseEntity<ErrorResponse> handleJwtVerification(
            JWTVerificationException ex, HttpServletRequest request) {
        log.warn("Token JWT inválido | path={} | causa={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Token de autenticação inválido ou expirado. Faça login novamente."));
    }

    /**
     * 403 – acesso negado pelo Spring Security (ex.: @Secured, @PreAuthorize).
     * Nota: AccessDeniedException lançada *dentro* do filtro de segurança
     * é tratada pelo accessDeniedHandler em SecurityConfigurations;
     * este handler cobre chamadas que chegam ao @ControllerAdvice.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleSpringAccessDenied(
            AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Acesso negado (Spring Security) | path={} method={} user={}",
                request.getRequestURI(), request.getMethod(), getCurrentUser());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse("Você não tem permissão para acessar este recurso."));
    }

    /** 403 – acesso negado pela lógica de negócio (AccessResourceDeniedException). */
    @ExceptionHandler(AccessResourceDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessResourceDeniedException exception, HttpServletRequest request) {
        if (auditService != null) {
            auditService.record(AuditEvent.builder()
                    .action("ACCESS_DENIED")
                    .actor(getCurrentUser())
                    .actorIp(request.getRemoteAddr())
                    .userAgent(request.getHeader("User-Agent"))
                    .extraData(request.getRequestURI())
                    .outcome(AuditOutcome.FAILURE)
                    .errorMessage(exception.getMessage())
                    .build());
        }
        log.warn("Acesso negado (negócio) | path={} user={} msg={}",
                request.getRequestURI(), getCurrentUser(), exception.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(exception.getMessage()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 2. Exceções de negócio / domínio
    // ──────────────────────────────────────────────────────────────────────────

    /** 404 – recurso de negócio não encontrado. */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> resourceNotFound(
            ResourceNotFoundException error, HttpServletRequest request) {
        log.warn("Recurso não encontrado | path={} | msg={}", request.getRequestURI(), error.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(error.getMessage()));
    }

    /** 409 – recurso já existente (duplicidade de negócio). */
    @ExceptionHandler(ResourceAlreadyExists.class)
    public ResponseEntity<ErrorResponse> alreadyExists(
            ResourceAlreadyExists error, HttpServletRequest request) {
        log.warn("Conflito de recurso | path={} | msg={}", request.getRequestURI(), error.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(error.getMessage()));
    }

    /** 409 – quarto indisponível na data solicitada. */
    @ExceptionHandler(RoomNotAvailable.class)
    public ResponseEntity<ErrorResponse> roomNotAvailable(
            RoomNotAvailable error, HttpServletRequest request) {
        log.warn("Quarto indisponível | path={} | msg={}", request.getRequestURI(), error.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(error.getMessage()));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 3. Exceções de persistência / JPA
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * 404 – EntityNotFoundException do JPA (ex.: entityManager.getReference()).
     * Retorna apenas 404 sem body para não expor detalhes internos.
     */
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(
            EntityNotFoundException ex, HttpServletRequest request) {
        log.warn("Entidade JPA não encontrada | path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("O recurso solicitado não foi encontrado."));
    }

    /**
     * 409 – violação de constraint de banco (unique, FK, not-null via DB).
     * A mensagem técnica do banco NUNCA é exposta.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade de dados | path={} | causa={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Operação não permitida: existe um conflito com os dados existentes."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 4. Exceções de validação e binding (400)
    // ──────────────────────────────────────────────────────────────────────────

    /** 400 – erros de validação de campo (@Valid / @Validated). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<List<FieldErrorDetail>> badRequest(
            MethodArgumentNotValidException error, HttpServletRequest request) {
        log.warn("Erro de validação de entrada | path={}", request.getRequestURI());
        var errors = error.getFieldErrors().stream().map(FieldErrorDetail::new).toList();
        return ResponseEntity.badRequest().body(errors);
    }

    /** 400 – corpo da requisição ilegível (JSON malformado, tipo incompatível). */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {
        log.warn("Corpo da requisição ilegível | path={} | causa={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("O corpo da requisição está malformado ou em formato inválido."));
    }

    /** 400 – parâmetro de query obrigatório ausente. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParam(
            MissingServletRequestParameterException ex, HttpServletRequest request) {
        log.warn("Parâmetro obrigatório ausente | path={} | param={}", request.getRequestURI(), ex.getParameterName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("O parâmetro '" + ex.getParameterName() + "' é obrigatório."));
    }

    /** 400 – tipo incompatível em parâmetro de path/query. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        log.warn("Tipo de parâmetro inválido | path={} | param={}", request.getRequestURI(), ex.getName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("O valor informado para '" + ex.getName() + "' é inválido."));
    }

    /** 400 – header obrigatório ausente. */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(
            MissingRequestHeaderException ex, HttpServletRequest request) {
        log.warn("Header obrigatório ausente | path={} | header={}", request.getRequestURI(), ex.getHeaderName());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("O header '" + ex.getHeaderName() + "' é obrigatório."));
    }

    /** 413 – arquivo enviado excede o tamanho máximo configurado. */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(
            MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Arquivo muito grande | path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(new ErrorResponse("O arquivo enviado excede o tamanho máximo permitido."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 5. Exceções de roteamento HTTP (405, 415, 406, 404)
    // ──────────────────────────────────────────────────────────────────────────

    /** 405 – método HTTP não suportado (ex.: GET onde só existe POST). */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        log.warn("Método HTTP não suportado | path={} method={}", request.getRequestURI(), request.getMethod());
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse("O método HTTP '" + ex.getMethod() + "' não é suportado para este recurso."));
    }

    /** 415 – Content-Type não suportado. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        log.warn("Content-Type não suportado | path={} contentType={}", request.getRequestURI(), ex.getContentType());
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse("O tipo de mídia da requisição não é suportado."));
    }

    /** 406 – Accept não aceito. */
    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotAcceptable(
            HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        log.warn("Accept não suportado | path={}", request.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(new ErrorResponse("O formato de resposta solicitado não é suportado."));
    }

    /** 404 – rota não encontrada (Spring MVC 6+). */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoResourceFound(
            NoResourceFoundException ex, HttpServletRequest request) {
        log.warn("Rota não encontrada | path={} method={}", request.getRequestURI(), request.getMethod());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse("O endereço solicitado não foi encontrado."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 6. Exceções de infraestrutura / storage
    // ──────────────────────────────────────────────────────────────────────────

    /** 503 – erro no serviço de armazenamento (MinIO). */
    @ExceptionHandler(MyCustomStorageException.class)
    public ResponseEntity<ErrorResponse> minioError(
            MyCustomStorageException error, HttpServletRequest request) {
        log.error("Erro no serviço de armazenamento | path={} | causa={}",
                request.getRequestURI(), error.getMessage());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(new ErrorResponse("O serviço de armazenamento de arquivos está temporariamente indisponível. Tente novamente em instantes."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 7. Exceções de runtime conhecidas (400)
    // ──────────────────────────────────────────────────────────────────────────

    /** 400 – argumento de negócio inválido. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Argumento inválido | path={} | causa={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.badRequest()
                .body(new ErrorResponse("Os dados informados são inválidos. Verifique os campos e tente novamente."));
    }

    /** 422 – estado inválido da aplicação detectado na camada de negócio. */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(
            IllegalStateException ex, HttpServletRequest request) {
        log.warn("Estado inválido | path={} | causa={}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorResponse("A operação não pode ser realizada no estado atual do recurso."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 8. Fallback genérico – NUNCA expõe detalhes técnicos
    // ──────────────────────────────────────────────────────────────────────────

    /** 500 – qualquer exceção não mapeada. Stack trace é logado, nunca exposto. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex, HttpServletRequest request) {
        log.error("Erro inesperado | path={} method={} user={}",
                request.getRequestURI(), request.getMethod(), getCurrentUser(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("Ocorreu um erro inesperado. Por favor, tente novamente mais tarde."));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Tipos internos reutilizáveis
    // ──────────────────────────────────────────────────────────────────────────

    /** Detalhe de erro de campo de validação (reutilizado em testes via JSON). */
    public record FieldErrorDetail(String field, String error) {
        public FieldErrorDetail(org.springframework.validation.FieldError fe) {
            this(fe.getField(), fe.getDefaultMessage());
        }
    }

    /** DTO canônico de erro – contrato com o front-end. */
    public record ErrorResponse(String message) {}

    // ──────────────────────────────────────────────────────────────────────────
    // Utilitário
    // ──────────────────────────────────────────────────────────────────────────

    private String getCurrentUser() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication())
                .map(Authentication::getName)
                .orElse("anonymous");
    }
}
