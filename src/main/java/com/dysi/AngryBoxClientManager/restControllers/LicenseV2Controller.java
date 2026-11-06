package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.LicenseQueryViews.Metadata;
import com.dysi.AngryBoxClientManager.security.LicenseQueryLogFilter;
import com.dysi.AngryBoxClientManager.servicesImpl.LicenseV2Service;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.security.Principal;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/clients/license")
@PreAuthorize("hasAnyRole('ADMIN','USER','SELLER')")
public class LicenseV2Controller {
    private final LicenseV2Service service;
    public LicenseV2Controller(LicenseV2Service service) { this.service = service; }

    /** Conserva los parámetros conocidos y agrega metadatos opcionales en el cuerpo JSON. */
    @PostMapping
    public ObjectNode license(@RequestParam String username, @RequestParam Long companyId,
            @Valid @RequestBody Metadata metadata, Principal principal, HttpServletRequest request) {
        request.setAttribute(LicenseQueryLogFilter.METADATA, metadata);
        return service.license(username, companyId, principal.getName());
    }
}
