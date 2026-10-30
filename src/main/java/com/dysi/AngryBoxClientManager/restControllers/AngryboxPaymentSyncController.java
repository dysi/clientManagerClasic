package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.AngryboxPaymentSync.*;
import com.dysi.AngryBoxClientManager.servicesImpl.AngryboxPaymentSyncService;
import java.util.List;
import java.util.Map;
import javax.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments/sync")
@PreAuthorize("hasAuthority(@angryboxSyncProperties.requiredAuthority.name())")
public class AngryboxPaymentSyncController {
    private final AngryboxPaymentSyncService service;

    public AngryboxPaymentSyncController(AngryboxPaymentSyncService service) {
        this.service = service;
    }

    @PostMapping("/matching")
    public List<Link> matching(@RequestParam String username, @RequestParam Long companyId,
            @Valid @RequestBody Matching input) {
        return service.matching(username, companyId, input);
    }

    @GetMapping("/pending")
    public Page pending(@RequestParam String username, @RequestParam Long companyId,
            @RequestParam(defaultValue = "0") long after, @RequestParam(required = false) Long until,
            @RequestParam(required = false) String desde) {
        return service.pending(username, companyId, after, until, desde);
    }

    @PostMapping("/ack")
    public Map<String, Boolean> acknowledge(@RequestParam String username, @RequestParam Long companyId,
            @Valid @RequestBody Ack input) {
        service.acknowledge(username, companyId, input);
        return Map.of("confirmed", true);
    }
}
