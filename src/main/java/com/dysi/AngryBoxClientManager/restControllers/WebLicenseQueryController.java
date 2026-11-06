package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.LicenseQueryViews.*;
import com.dysi.AngryBoxClientManager.servicesImpl.LicenseQueryLogService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/api/license-query-log")
@PreAuthorize("hasRole('ADMIN')")
public class WebLicenseQueryController {
    private final LicenseQueryLogService service;
    public WebLicenseQueryController(LicenseQueryLogService service) { this.service = service; }

    @InitBinder("filter") public void filterBinding(WebDataBinder binder) {
        binder.initDirectFieldAccess();
        binder.setAllowedFields("keyword", "companyId", "licenseId", "ip", "result", "version", "from", "to");
    }
    @GetMapping public Page<Entry> entries(@Valid @ModelAttribute("filter") Filter filter,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.entries(filter, page, size);
    }
    @GetMapping("/summary") public Summary summary(@Valid @ModelAttribute("filter") Filter filter) { return service.summary(filter); }
    @GetMapping("/origins") public List<Origin> origins(@Valid @ModelAttribute("filter") Filter filter) { return service.origins(filter); }
}
