package com.dysi.AngryBoxClientManager.restControllers;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;

/** El multipart puede fallar antes de que Spring determine el controlador destinatario. */
@RestControllerAdvice
public class WebUploadErrorHandler {
    private final org.springframework.boot.autoconfigure.web.servlet.MultipartProperties limits;
    public WebUploadErrorHandler(org.springframework.boot.autoconfigure.web.servlet.MultipartProperties limits) { this.limits = limits; }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> size(Exception error) {
        return ResponseEntity.status(413).body(Map.of("message", "Cada archivo no puede superar los " + limits.getMaxFileSize().toMegabytes() + " MB y el envío completo no puede superar los " + limits.getMaxRequestSize().toMegabytes() + " MB."));
    }
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<?> multipart(Exception error) {
        return ResponseEntity.badRequest().body(Map.of("message", "No se pudo leer la presentación. Volvé a seleccionar los comprobantes e intentá nuevamente."));
    }
}
