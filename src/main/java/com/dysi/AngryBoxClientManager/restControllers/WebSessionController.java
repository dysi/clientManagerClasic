package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.security.JwtUser;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import com.dysi.AngryBoxClientManager.security.DocumentLoginLimiter;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalAccessService;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/web/session")
public class WebSessionController {
    private final AuthenticationManager authenticationManager;
    private final PortalAccessService portal;
    private final DocumentLoginLimiter limiter;
    private final com.dysi.AngryBoxClientManager.security.TurnstileService turnstile;
    private final com.dysi.AngryBoxClientManager.config.SupportContactProperties support;

    public WebSessionController(AuthenticationManager authenticationManager,
            PortalAccessService portal,
            DocumentLoginLimiter limiter,
            com.dysi.AngryBoxClientManager.security.TurnstileService turnstile,
            com.dysi.AngryBoxClientManager.config.SupportContactProperties support) {
        this.turnstile = turnstile;
        this.authenticationManager = authenticationManager;
        this.portal = portal;
        this.limiter = limiter; this.support = support;
    }

    /** Únicamente el contacto público necesario antes de iniciar sesión. */
    @GetMapping("/support")
    public Map<String, String> support(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("whatsappUrl", support.getWhatsappUrl());
    }

    /** Solo configuración pública; nunca devuelve la clave secreta. */
    @GetMapping("/turnstile")
    public Map<String, Object> turnstile(HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("enabled", turnstile.isEnabled(), "siteKey", turnstile.getSiteKey());
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Credentials credentials,
            HttpServletRequest request, HttpServletResponse response) {
        turnstile.verify(credentials.turnstileToken, "password_login");
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credentials.username, credentials.password));
            JwtUser user = (JwtUser) authentication.getPrincipal();
            if (!portal.isActiveUser(user)) {
                throw new DisabledException("Cuenta no disponible");
            }
            if (authentication.getAuthorities().stream().noneMatch(role -> role.getAuthority().equals("ROLE_ADMIN"))) {
                PortalPrincipal principal = portal.authenticatedLogin(user);
                authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            }
            return establish(authentication, request, response);
        } catch (AuthenticationException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        }
    }

    @PostMapping("/document")
    public Map<String, Object> document(@Valid @RequestBody DocumentCredentials credentials,
            HttpServletRequest request, HttpServletResponse response) {
        limiter.check(request.getRemoteAddr());
        turnstile.verify(credentials.turnstileToken, "document_login");
        PortalPrincipal principal =
                portal.documentLogin(credentials.type, credentials.document);
        return establish(new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()), request, response);
    }

    private Map<String, Object> establish(Authentication authentication, HttpServletRequest request,
            HttpServletResponse response) {
        // Reemplazar el principal y renovar el identificador y el CSRF en cada ingreso.
        request.getSession(true);
        request.changeSessionId();
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context, request, response);
        new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository().saveToken(null, request, response);
        response.setHeader("Cache-Control", "no-store");
        return describe(authentication);
    }

    @GetMapping
    public Map<String, Object> session(Authentication authentication, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return describe(authentication);
    }

    private Map<String, Object> describe(Authentication authentication) {
        Map<String, Object> user = new LinkedHashMap<>();
        boolean restricted = authentication.getPrincipal() instanceof PortalPrincipal;
        user.put("username", restricted ? "Portal de clientes" : authentication.getName());
        user.put("roles", authentication.getAuthorities().stream().map(Object::toString).collect(Collectors.toList()));
        user.put("access", restricted ? "PORTAL" : "ADMIN");
        if (restricted) user.put("origin", ((PortalPrincipal)
                authentication.getPrincipal()).getOrigin().name());
        return user;
    }

    public static class Credentials {
        @Size(max = 2048) public String turnstileToken;
        @NotBlank @Size(max = 50) public String username;
        @NotBlank @Size(max = 200) public String password;
    }

    public static class DocumentCredentials {
        @Size(max = 2048) public String turnstileToken;
        // Opcional para compatibilidad; el servidor detecta el tipo por longitud.
        public PortalAccessService.DocumentType type;
        @NotBlank @Size(max = 32) public String document;
    }
}
