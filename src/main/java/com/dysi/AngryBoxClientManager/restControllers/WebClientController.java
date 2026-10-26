package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.ClientDTO;
import com.dysi.AngryBoxClientManager.dtos.ClientInput;
import com.dysi.AngryBoxClientManager.models.Client;
import com.dysi.AngryBoxClientManager.repositories.ClientRepository;
import java.net.URI;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
    public WebClientController(ClientRepository clients) { this.clients = clients; }

    @GetMapping
    public Page<ClientDTO> list(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        if (page < 0 || size < 1 || size > 100 || keyword.length() > 150) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetros de búsqueda inválidos.");
        }
        String escaped = keyword.trim().toLowerCase(java.util.Locale.ROOT)
                .replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return clients.searchForWeb("%" + escaped + "%", PageRequest.of(page, size,
                Sort.by("lastname", "firstname", "id"))).map(this::dto);
    }

    @GetMapping("/{id}")
    public ClientDTO detail(@PathVariable long id) { return dto(find(id)); }

    @PostMapping
    @Transactional
    public ResponseEntity<ClientDTO> create(@Valid @RequestBody ClientInput input) {
        Client client = new Client();
        fill(client, input);
        clients.saveAndFlush(client);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().path("/{id}").buildAndExpand(client.getId()).toUri();
        return ResponseEntity.created(location).body(dto(client));
    }

    @PutMapping("/{id}")
    @Transactional
    public ClientDTO update(@PathVariable long id, @Valid @RequestBody ClientInput input) {
        Client client = find(id);
        fill(client, input);
        return dto(clients.saveAndFlush(client));
    }

    private Client find(long id) {
        return clients.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el cliente."));
    }
    private void fill(Client client, ClientInput input) {
        client.setFirstname(input.firstname.trim()); client.setLastname(input.lastname.trim());
        client.setEmail(input.email.trim()); client.setPhone(input.phone.trim()); client.setCuil(input.cuil.trim());
    }
    private ClientDTO dto(Client client) {
        return new ClientDTO(client.getId(), client.getFirstname(), client.getLastname(), client.getEmail(),
                client.getPhone(), client.getCuil(), client.getUser() == null ? null : client.getUser().getUsername());
    }
}
