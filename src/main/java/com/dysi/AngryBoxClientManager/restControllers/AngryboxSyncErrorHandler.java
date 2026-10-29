package com.dysi.AngryBoxClientManager.restControllers;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/** Respuestas del lote sin cuerpos de clientes, credenciales ni detalles de infraestructura. */
@RestControllerAdvice(assignableTypes = AngryboxClientSyncController.class)
public class AngryboxSyncErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(AngryboxSyncErrorHandler.class);

    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<?> forbidden(Exception error) {
        return ResponseEntity.status(403).body(Map.of("message", "El usuario no tiene el permiso requerido para usar esta API."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of("message", error.getReason()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class,
            org.springframework.http.converter.HttpMessageNotReadableException.class,
            org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class,
            org.springframework.web.bind.MissingServletRequestParameterException.class})
    public ResponseEntity<?> invalid(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message",
                "Revisá el contrato v1: UUID válido, empresa positiva, fecha con zona horaria y lista de clientes obligatorios. No se procesó el lote."));
    }

    @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,
            org.springframework.dao.PessimisticLockingFailureException.class,
            javax.persistence.OptimisticLockException.class})
    public ResponseEntity<?> concurrent(Exception error) {
        return ResponseEntity.status(409).body(Map.of("message",
                "El lote entró en conflicto con otra operación. Reintentá el mismo UUID con los mismos datos para recuperar su resultado."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> failed(Exception error) {
        logger.error("No se pudo confirmar un lote Angrybox; tipo={}", error.getClass().getSimpleName());
        return ResponseEntity.status(503).body(Map.of("message",
                "No se pudo confirmar la sincronización. Conservá el UUID y los datos originales antes de reintentar."));
    }
}
