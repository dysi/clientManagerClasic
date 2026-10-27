package com.dysi.AngryBoxClientManager.restControllers;

import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.http.converter.HttpMessageNotReadableException;

@RestControllerAdvice(assignableTypes = { WebSessionController.class, WebClientController.class, WebAdminController.class, WebParameterController.class, WebPortalController.class, WebReceiptController.class, WebPaymentReviewController.class })
public class WebErrorHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> validation(MethodArgumentNotValidException error) {
        Map<String, String> fields = new LinkedHashMap<>();
        error.getBindingResult().getFieldErrors().forEach(field -> fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return ResponseEntity.badRequest().body(Map.of("message", "Revisá los campos indicados.", "fields", fields));
    }
    @ExceptionHandler({org.springframework.orm.ObjectOptimisticLockingFailureException.class, javax.persistence.OptimisticLockException.class, org.springframework.dao.PessimisticLockingFailureException.class})
    public ResponseEntity<?> concurrent(Exception error) {
        return ResponseEntity.status(409).body(Map.of("message", "El registro cambió o está siendo revisado. Actualizá el detalle y volvé a intentar."));
    }
    /** Los índices únicos resuelven también carreras entre dos solicitudes de alta. */
    @ExceptionHandler(org.springframework.dao.DataIntegrityViolationException.class)
    public ResponseEntity<?> integrity(org.springframework.dao.DataIntegrityViolationException error) {
        return ResponseEntity.status(409).body(Map.of("message", "Los datos están duplicados o entraron en conflicto con otro registro. Actualizá la página y revisalos."));
    }

    @ExceptionHandler({ javax.persistence.PersistenceException.class, org.springframework.transaction.TransactionException.class })
    public ResponseEntity<?> persistence(Exception error) {
        return ResponseEntity.status(503).body(Map.of("message", "No se pudo confirmar la operación. Conservá el borrador y volvé a intentar con la misma referencia."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<?> status(ResponseStatusException error) {
        return ResponseEntity.status(error.getStatus()).body(Map.of("message", error.getReason() == null ? "No se pudo completar la operación." : error.getReason()));
    }
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, HttpMessageNotReadableException.class})
    public ResponseEntity<?> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "La solicitud contiene datos inválidos."));
    }

    @ExceptionHandler(org.springframework.web.multipart.support.MissingServletRequestPartException.class)
    public ResponseEntity<?> multipart(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "No se pudo leer la presentación. Volvé a seleccionar los comprobantes e intentá nuevamente."));
    }
}
