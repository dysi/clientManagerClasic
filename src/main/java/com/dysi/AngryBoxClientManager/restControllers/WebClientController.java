package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.WebClientView;
import com.dysi.AngryBoxClientManager.servicesImpl.WebClientReadService;
import com.dysi.AngryBoxClientManager.dtos.ClientInput;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import java.net.URI;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/web/api/clients")
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class WebClientController {
    private final ClientRepository clients;
    private final WebClientReadService reads;
    private final com.dysi.AngryBoxClientManager.servicesImpl.ClientAddressService addresses;
    public WebClientController(ClientRepository clients, WebClientReadService reads,
            com.dysi.AngryBoxClientManager.servicesImpl.ClientAddressService addresses) {
        this.addresses = addresses;
        this.clients = clients; this.reads = reads;
    }

    @GetMapping("/countries")
    public java.util.List<com.dysi.AngryBoxClientManager.servicesImpl.ClientAddressService.Place> countries() { return addresses.countries(); }
    @GetMapping("/provinces")
    public java.util.List<com.dysi.AngryBoxClientManager.servicesImpl.ClientAddressService.Place> provinces() { return addresses.provinces(); }

    @GetMapping
    public Page<WebClientView> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String balanceState,
            @RequestParam(defaultValue = "ACTIVE") String status,
            @RequestParam(required = false) Integer staleDays,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        if (!java.util.Arrays.asList("ALL", "ACTIVE", "REMOVED").contains(status)
                || !java.util.Arrays.asList("ALL", "DEBT", "CREDIT", "ZERO", "UNKNOWN").contains(balanceState)
                || (staleDays != null && !java.util.Arrays.asList(15, 30, 45, 60).contains(staleDays))
                || page < 0 || size < 1 || size > 100 || keyword.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetros de búsqueda inválidos.");
        }
        return reads.list(keyword, balanceState, status, staleDays, page, size);
    }

    @GetMapping("/{id}")
    public WebClientView detail(@PathVariable long id) { return dto(find(id)); }

    @PostMapping
    @Transactional
    public ResponseEntity<WebClientView> create(@Valid @RequestBody ClientInput input) {
        Client client = new Client();
        fill(client, input);
        clients.saveAndFlush(client);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(client.getId()).toUri();
        return ResponseEntity.created(location).body(dto(client));
    }

    @PutMapping("/{id}")
    @Transactional
    public WebClientView update(@PathVariable long id, @Valid @RequestBody ClientInput input) {
        Client client = find(id);
        fill(client, input);
        return dto(clients.saveAndFlush(client));
    }

    @PutMapping("/{id}/status")
    @Transactional
    public WebClientView status(@PathVariable long id,
            @Valid @RequestBody com.dysi.AngryBoxClientManager.dtos.AdminInputs.ClientStatusInput input) {
        return reads.status(id, input.removed);
    }

    private Client find(long id) {
        return clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el cliente."));
    }
    private void fill(Client client, ClientInput input) {
        addresses.fill(client, input);
        client.setFirstname(input.firstname.trim()); client.setLastname(input.lastname.trim());
        client.setEmail(input.email.trim()); client.setPhone(input.phone.trim()); client.setCuil(input.cuil.trim());
        if (input.dni != null) client.setDni(input.dni.isEmpty() ? null : input.dni);
        if (input.legalName != null) client.setLegalName(input.legalName);
        if (input.tradeName != null) client.setTradeName(input.tradeName);
        if (input.externalCode != null) client.setExternalCode(input.externalCode);
    }
    private WebClientView dto(Client client) {
        return reads.view(client);
    }
}
