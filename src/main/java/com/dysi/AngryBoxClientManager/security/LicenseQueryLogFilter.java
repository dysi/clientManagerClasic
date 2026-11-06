package com.dysi.AngryBoxClientManager.security;

import com.dysi.AngryBoxClientManager.dtos.LicenseQueryViews.Metadata;
import com.dysi.AngryBoxClientManager.models.LicenseQueryLog;
import com.dysi.AngryBoxClientManager.servicesImpl.LicenseQueryLogService;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.time.Instant;
import javax.servlet.*;
import javax.servlet.http.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Se ejecuta dentro del contexto de seguridad e incluye rechazos previos al controlador. */
public class LicenseQueryLogFilter extends OncePerRequestFilter {
    public static final String RESPONSE = LicenseQueryLogFilter.class.getName() + ".response";
    public static final String METADATA = LicenseQueryLogFilter.class.getName() + ".metadata";
    private static final Logger LOG = LoggerFactory.getLogger(LicenseQueryLogFilter.class);
    private final LicenseQueryLogService service;

    public LicenseQueryLogFilter(LicenseQueryLogService service) { this.service = service; }

    public static boolean isLicensePath(String path) {
        return path.equals("/api/clients/license") || path.equals("/api/clients/license/")
                || path.equals("/api/v2/clients/license") || path.equals("/api/v2/clients/license/");
    }

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        return !isLicensePath(request.getRequestURI().substring(request.getContextPath().length()));
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {
        Instant received = Instant.now();
        long start = System.nanoTime();
        boolean completed = false;
        try {
            chain.doFilter(request, response);
            completed = true;
        } finally {
            // La telemetría nunca cambia el resultado de una consulta, incluso si falla su almacenamiento.
            try {
                LicenseQueryLog entry = new LicenseQueryLog();
                entry.setReceivedAt(received);
                entry.setDurationMs(Math.max(0, (System.nanoTime() - start) / 1_000_000));
                entry.setHttpStatus(completed ? response.getStatus() : 500);
                entry.setHttpMethod(clean(request.getMethod(), 16));
                entry.setApiVersion(request.getRequestURI().contains("/api/v2/") ? "v2" : "v1");
                // No se confía en X-Forwarded-For ni se almacenan cabeceras arbitrarias.
                entry.setRemoteIp(clean(request.getRemoteAddr(), 64));
                entry.setUserAgent(clean(request.getHeader("User-Agent"), 300));
                entry.setRequestedUsername(clean(request.getParameter("username"), 150));
                entry.setCompanyId(companyId(request.getParameter("companyId")));
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken))
                    entry.setAuthenticatedUsername(clean(auth.getName(), 50));
                Object snapshot = request.getAttribute(RESPONSE);
                if (completed && response.getStatus() == 200 && snapshot instanceof JsonNode) {
                    JsonNode license = (JsonNode) snapshot;
                    if (license.path("id").canConvertToLong()) entry.setLicenseId(license.path("id").longValue());
                    entry.setCompanyName(clean(license.path("companyName").asText(null), 255));
                    // Fotografía del primer estado que consume AngryBox2, incluida la extensión efectiva.
                    JsonNode state = license.path("licenseStatuses").path(0);
                    if (state.path("forcedLock").isBoolean()) entry.setForcedLock(state.path("forcedLock").booleanValue());
                    if (state.path("lockDate").canConvertToLong()) entry.setLockDateEpochMs(state.path("lockDate").longValue());
                }
                Object metadata = request.getAttribute(METADATA);
                if (metadata instanceof Metadata) {
                    Metadata value = (Metadata) metadata;
                    entry.setInstallationId(clean(value.installationId, 100));
                    entry.setProcessId(clean(value.processId, 100));
                    entry.setHostname(clean(value.hostname, 255));
                    entry.setAppVersion(clean(value.appVersion, 100));
                    entry.setOperatingSystem(clean(value.operatingSystem, 100));
                }
                service.record(entry);
            } catch (RuntimeException failure) {
                // No registrar datos de la petición ni mensajes SQL que puedan contenerlos.
                LOG.error("No se pudo persistir una consulta de licencia en la bitácora ({}).", failure.getClass().getSimpleName());
            }
        }
    }

    private static Long companyId(String value) {
        try { return value == null ? null : Long.valueOf(value); }
        catch (NumberFormatException ignored) { return null; }
    }

    private static String clean(String value, int max) {
        if (value == null) return null;
        String result = value.replaceAll("[\\p{Cntrl}]", " ").trim();
        return result.isEmpty() ? null : result.substring(0, Math.min(max, result.length()));
    }
}
