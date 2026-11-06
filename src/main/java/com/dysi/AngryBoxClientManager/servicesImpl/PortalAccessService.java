package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.models.OperationOrigin;
import com.dysi.AngryBoxClientManager.models.User;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import com.dysi.AngryBoxClientManager.repositories.UserRepository;
import com.dysi.AngryBoxClientManager.security.JwtUser;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Resuelve el cliente en servidor y comprueba ambos lados del vínculo en cada petición. */
@Service
@Transactional(readOnly = true)
public class PortalAccessService {
    public enum DocumentType { DNI, CUIT }
    private final ClientRepository clients;
    private final UserRepository users;

    public PortalAccessService(ClientRepository clients, UserRepository users) {
        this.clients = clients;
        this.users = users;
    }

    public PortalPrincipal documentLogin(DocumentType type, String value) {
        // Solo se aceptan separadores conocidos; nunca descartar letras silenciosamente.
        if (value == null || !value.matches("[0-9 .-]+")) invalidDocument();
        String document = value.replaceAll("[ .-]", "");
        if (type == null || !document.matches(type == DocumentType.DNI ? "[0-9]{1,8}" : "[0-9]{11}")) {
            invalidDocument();
        }
        String dni = type == DocumentType.DNI ? String.format("%8s", document).replace(' ', '0') : "";
        List<Long> matches = clients.findPortalDocumentMatches(type.name(), document, dni, PageRequest.of(0, 2));
        if (matches.size() != 1) throw denied();
        Client client = clients.findById(matches.get(0)).orElseThrow(this::denied);
        User user = associatedUser(client);
        return new PortalPrincipal(client.getId(), user, OperationOrigin.PORTAL_DOCUMENTO);
    }

    public PortalPrincipal authenticatedLogin(JwtUser authenticated) {
        User user = users.findById(authenticated.getId()).orElseThrow(this::denied);
        List<Long> matches = clients.findPortalClientsForUser(user.getId(), PageRequest.of(0, 2));
        if (matches.size() != 1) throw denied();
        Client client = clients.findById(matches.get(0)).orElseThrow(this::denied);
        associatedUser(client);
        return new PortalPrincipal(client.getId(), user, OperationOrigin.ACCESO_AUTENTICADO);
    }

    /** El documento identifica la cuenta, pero no acredita al titular. */
    public void requireSensitiveAccess(PortalPrincipal principal) {
        if (revalidate(principal).getOrigin() != OperationOrigin.ACCESO_AUTENTICADO) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Ingresá con usuario y contraseña para consultar esta información.");
        }
    }

    public boolean isActiveUser(JwtUser principal) {
        return users.findById(principal.getId())
                .filter(user -> user.getRemoveDate() == null && Boolean.TRUE.equals(user.getEnabled())).isPresent();
    }

    public PortalPrincipal revalidate(PortalPrincipal principal) {
        Client client = clients.findById(principal.getClientId()).orElseThrow(this::denied);
        User user = associatedUser(client);
        if (!Objects.equals(user.getId(), principal.getId())
                || !Objects.equals(user.getLastPasswordResetDate(), principal.getLastPasswordResetDate())) {
            throw denied();
        }
        return new PortalPrincipal(client.getId(), user, principal.getOrigin());
    }

    public Map<String, String> context(PortalPrincipal principal) {
        PortalPrincipal current = revalidate(principal);
        Client client = clients.findById(current.getClientId()).orElseThrow(this::denied);
        // No documentos, contacto, auditoría ni permisos del usuario asociado.
        return Map.of("name", client.getFirstname() == null ? "Cliente" : client.getFirstname(),
                "origin", current.getOrigin().name());
    }

    private User associatedUser(Client client) {
        User user = client.getUser();
        if (user == null || user.getRemoveDate() != null
                || !Boolean.TRUE.equals(user.getEnabled()) || user.getClient() == null
                || !Objects.equals(user.getClient().getId(), client.getId())) throw denied();
        List<Long> matches = clients.findPortalClientsForUser(user.getId(), PageRequest.of(0, 2));
        if (matches.size() != 1 || !Objects.equals(matches.get(0), client.getId())) throw denied();
        return user;
    }

    private void invalidDocument() {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Revisá el tipo y formato del documento.");
    }

    private ResponseStatusException denied() {
        // Misma respuesta para ausencia, ambigüedad y cuentas sin acceso válido.
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                "No se pudo habilitar el acceso al portal. Contactá a un administrador para revisar tus datos y el usuario vinculado.");
    }
}
