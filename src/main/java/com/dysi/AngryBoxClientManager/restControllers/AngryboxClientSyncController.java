package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.AngryboxClientSync;
import com.dysi.AngryBoxClientManager.servicesImpl.AngryboxClientSyncService;
import javax.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/clients/sync")
public class AngryboxClientSyncController {
    private final AngryboxClientSyncService service;

    public AngryboxClientSyncController(AngryboxClientSyncService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority(@angryboxSyncProperties.requiredAuthority.name())")
    public AngryboxClientSync.Response synchronize(@RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestParam String username, @RequestParam Long companyId,
            @Valid @RequestBody AngryboxClientSync.Request input) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Esta operación requiere el acceso de integración por Bearer.");
        }
        if (!companyId.equals(input.companyId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "companyId debe coincidir en la URL y en el cuerpo del envío.");
        }
        return service.synchronize(username, input);
    }
}
