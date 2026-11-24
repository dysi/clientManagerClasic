package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.security.JwtUser;
import com.dysi.AngryBoxClientManager.servicesImpl.PortalTicketService;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Contrato servidor a servidor, autenticado exclusivamente mediante Bearer. */
@RestController
public class PortalTicketController {
    private final PortalTicketService tickets;

    public PortalTicketController(PortalTicketService tickets) {
        this.tickets = tickets;
    }

    @PostMapping("/api/portal-access/tickets")
    @PreAuthorize("isAuthenticated() and !(principal instanceof T(com.dysi.AngryBoxClientManager.security.PortalPrincipal))")
    public Map<String, String> issue(@AuthenticationPrincipal JwtUser user,
            @Valid @RequestBody Input input, HttpServletResponse response) {
        response.setHeader("Cache-Control", "no-store");
        return Map.of("ticket", tickets.issue(user, input.companyId));
    }

    public static class Input {
        @NotNull @Positive public Long companyId;
    }
}
