package com.dysi.AngryBoxClientManager.security;

import java.time.Duration;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/** Verifica antes de autenticar; los errores del proveedor nunca conceden acceso. */
@Service
public class TurnstileService {
    private final boolean enabled;
    private final String siteKey;
    private final String secretKey;
    private final Set<String> hostnames;
    private final RestTemplate client;

    public TurnstileService(RestTemplateBuilder builder,
            @Value("${portal.turnstile.enabled:true}") boolean enabled,
            @Value("${portal.turnstile.site-key:}") String siteKey,
            @Value("${portal.turnstile.secret-key:}") String secretKey,
            @Value("${portal.turnstile.hostnames:}") String hostnames) {
        this.enabled = enabled;
        this.siteKey = siteKey.trim();
        this.secretKey = secretKey.trim();
        this.hostnames = Arrays.stream(hostnames.split(",")).map(String::trim)
                .filter(value -> !value.isEmpty()).collect(Collectors.toSet());
        this.client = builder.setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5)).build();
    }

    public boolean isEnabled() { return enabled; }
    public String getSiteKey() { return siteKey; }

    public void verify(String token, String action) {
        if (!enabled) return;
        if (siteKey.isEmpty() || secretKey.isEmpty() || hostnames.isEmpty()) unavailable();
        if (token == null || token.isBlank() || token.length() > 2048) invalid();
        LinkedMultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secretKey);
        form.add("response", token);
        Verification result;
        try {
            // No enviar documentos, contraseñas ni datos del cliente al proveedor.
            result = client.postForObject("https://challenges.cloudflare.com/turnstile/v0/siteverify",
                    form, Verification.class);
        } catch (RestClientException error) {
            // No exponer el cuerpo de error del proveedor ni sus datos sensibles.
            unavailable();
            return;
        }
        if (result == null) unavailable();
        if (!Boolean.TRUE.equals(result.success) || !action.equals(result.action)
                || !hostnames.contains(result.hostname)) invalid();
    }

    private void invalid() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "No se pudo validar la verificación de seguridad. Volvé a completarla.");
    }

    private void unavailable() {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "La verificación de seguridad no está disponible. Esperá un momento y volvé a intentar.");
    }

    public static class Verification {
        public Boolean success;
        public String hostname;
        public String action;
    }
}
