package com.dysi.AngryBoxClientManager.restControllers;

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

    public WebSessionController(AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/login")
    public Map<String, Object> login(@Valid @RequestBody Credentials credentials,
            HttpServletRequest request, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(credentials.username, credentials.password));
            request.getSession();
            request.changeSessionId();
            SecurityContext context = SecurityContextHolder.createEmptyContext();
            context.setAuthentication(authentication);
            SecurityContextHolder.setContext(context);
            new HttpSessionSecurityContextRepository().saveContext(context, request, response);
            new org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository().saveToken(null, request, response);
            response.setHeader("Cache-Control", "no-store");
            return describe(authentication);
        } catch (AuthenticationException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario o contraseña incorrectos.");
        }
    }

    @GetMapping
    public Map<String, Object> session(Authentication authentication, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return describe(authentication);
    }

    private Map<String, Object> describe(Authentication authentication) {
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("username", authentication.getName());
        user.put("roles", authentication.getAuthorities().stream().map(Object::toString).collect(Collectors.toList()));
        return user;
    }

    public static class Credentials {
        @NotBlank @Size(max = 50) public String username;
        @NotBlank @Size(max = 200) public String password;
    }
}
