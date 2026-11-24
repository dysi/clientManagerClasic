package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.License;
import com.dysi.AngryBoxClientManager.repositories.LicenseRepository;
import com.dysi.AngryBoxClientManager.security.JwtUser;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Accesos efímeros al portal; nunca transportan el JWT ni los roles de la API. */
@Service
public class PortalTicketService {
    private final PortalAccessService access;
    private final LicenseRepository licenses;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Ticket> pending = new HashMap<>();
    private static final int MAX_PENDING = 1000;
    private static final long LIFETIME_SECONDS = 60;

    public PortalTicketService(PortalAccessService access, LicenseRepository licenses) {
        this.access = access;
        this.licenses = licenses;
    }

    public String issue(JwtUser user, Long companyId) {
        PortalPrincipal principal = access.authenticatedLogin(user);
        validateCompany(principal, companyId);
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        synchronized (pending) {
            Instant now = Instant.now();
            Iterator<Ticket> iterator = pending.values().iterator();
            while (iterator.hasNext()) {
                if (!iterator.next().expiresAt.isAfter(now)) {
                    iterator.remove();
                }
            }
            if (pending.size() >= MAX_PENDING) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudo preparar el acceso. Volvé a intentar en un minuto.");
            }
            pending.put(value, new Ticket(principal, companyId, now.plusSeconds(LIFETIME_SECONDS)));
        }
        return value;
    }

    public PortalPrincipal consume(String value) {
        Ticket ticket;
        // La extracción atómica impide dos canjes simultáneos del mismo acceso.
        synchronized (pending) {
            ticket = pending.remove(value);
        }
        if (ticket == null || !ticket.expiresAt.isAfter(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "El acceso venció o ya fue utilizado. Volvé a abrir el portal desde Angrybox.");
        }
        PortalPrincipal principal = access.revalidate(ticket.principal);
        validateCompany(principal, ticket.companyId);
        return principal;
    }

    private void validateCompany(PortalPrincipal principal, Long companyId) {
        int matches = 0;
        boolean owner = false;
        for (License license : licenses.findLicensesByCompanyId(companyId)) {
            if (license.getRemoveDate() == null) {
                matches++;
                owner = !license.isHidden() && license.getClient() != null
                        && principal.getClientId().equals(license.getClient().getId());
            }
        }
        if (matches != 1 || !owner) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "La cuenta configurada no tiene acceso a la empresa indicada.");
        }
    }

    private static final class Ticket {
        private final PortalPrincipal principal;
        private final Long companyId;
        private final Instant expiresAt;

        private Ticket(PortalPrincipal principal, Long companyId, Instant expiresAt) {
            this.principal = principal;
            this.companyId = companyId;
            this.expiresAt = expiresAt;
        }
    }
}
