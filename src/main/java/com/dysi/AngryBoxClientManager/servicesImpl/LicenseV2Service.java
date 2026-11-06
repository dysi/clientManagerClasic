package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class LicenseV2Service {
    private final UserRepository users;
    private final LicenseRepository licenses;
    private final LicenseExtensionService extensions;
    public LicenseV2Service(UserRepository users, LicenseRepository licenses, LicenseExtensionService extensions) {
        this.users = users; this.licenses = licenses; this.extensions = extensions;
    }
    public ObjectNode license(String username, Long companyId, String actor) {
        // v2 no hereda la selección arbitraria de usuario de la API histórica.
        if (!actor.equals(username)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "El usuario solicitado no coincide con el autenticado.");
        User user = users.findByUsername(actor);
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || user.getRemoveDate() != null)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La cuenta no está habilitada.");
        if (companyId == null || companyId <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ingresá una empresa válida.");
        Client client = user.getClient();
        if (client == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "La cuenta no tiene un cliente asociado.");
        License license = licenses.findLicensesByClient(client).stream()
                .filter(value -> companyId.equals(value.getCompanyId())).findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró la licencia para esta cuenta."));
        return extensions.licenseResponse(client, license);
    }
}
