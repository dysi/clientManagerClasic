package com.dysi.AngryBoxClientManager.security;

import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Ventana por dirección remota, sin almacenar documentos ni confiar en cabeceras de proxy. */
@Component
public class DocumentLoginLimiter {
    private final Map<String, Window> windows = new HashMap<>();
    private final int attempts;
    private final long windowMillis;

    public DocumentLoginLimiter(@Value("${portal.document.max-attempts:10}") int attempts,
            @Value("${portal.document.window-seconds:900}") long windowSeconds) {
        if (attempts < 1 || windowSeconds < 1 || windowSeconds > 86400) {
            throw new IllegalArgumentException("Los límites del ingreso por documento deben ser positivos.");
        }
        this.attempts = attempts;
        this.windowMillis = windowSeconds * 1000;
    }

    public synchronized void check(String remoteAddress) {
        long now = System.currentTimeMillis();
        windows.entrySet().removeIf(entry -> entry.getValue().expiresAt <= now);
        Window window = windows.get(remoteAddress);
        // No expulsar ventanas vigentes: rotar direcciones no debe reiniciar el límite.
        if (window == null) {
            if (windows.size() >= 10000) reject();
            window = new Window(now + windowMillis);
            windows.put(remoteAddress, window);
        }
        if (window.used >= attempts) reject();
        window.used++;
    }

    private void reject() {
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Se alcanzó el límite de intentos. Esperá unos minutos antes de volver a ingresar.");
    }

    private static class Window {
        final long expiresAt;
        int used;
        Window(long expiresAt) { this.expiresAt = expiresAt; }
    }
}
