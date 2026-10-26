package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.AdminInputs.*;
import com.dysi.AngryBoxClientManager.dtos.AdminViews.*;
import com.dysi.AngryBoxClientManager.servicesImpl.WebAdminService;
import java.security.Principal;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/api")
@PreAuthorize("hasRole('ADMIN')")
public class WebAdminController {
    private final WebAdminService service;
    public WebAdminController(WebAdminService service) { this.service = service; }

    @GetMapping("/licenses") public Page<LicenseView> licenses(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long clientId, @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.licenses(keyword, clientId, status, page, size);
    }
    @GetMapping("/licenses/{id}") public LicenseView license(@PathVariable long id) { return service.license(id); }
    @PostMapping("/licenses") @ResponseStatus(HttpStatus.CREATED)
    public LicenseView createLicense(@Valid @RequestBody LicenseInput input) { return service.saveLicense(null, input); }
    @PutMapping("/licenses/{id}") public LicenseView updateLicense(@PathVariable long id, @Valid @RequestBody LicenseInput input) { return service.saveLicense(id, input); }
    @PutMapping("/licenses/{id}/lock") public LicenseView lock(@PathVariable long id, @Valid @RequestBody LockInput input) { return service.lockLicense(id, input.locked); }

    @GetMapping("/payments") public Page<PaymentView> payments(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long licenseId, @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.payments(keyword, licenseId, status, page, size);
    }
    @GetMapping("/payments/{id}") public PaymentView payment(@PathVariable long id) { return service.payment(id); }
    @PostMapping("/payments") @ResponseStatus(HttpStatus.CREATED)
    public PaymentView createPayment(@Valid @RequestBody PaymentInput input) { return service.savePayment(null, input); }
    @PutMapping("/payments/{id}") public PaymentView updatePayment(@PathVariable long id, @Valid @RequestBody PaymentInput input) { return service.savePayment(id, input); }
    @PutMapping("/payments/{id}/status") public PaymentView paymentStatus(@PathVariable long id, @Valid @RequestBody PaymentStatusInput input) { return service.paymentStatus(id, input.removed); }

    @GetMapping("/users") public Page<UserView> users(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long clientId, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.users(keyword, clientId, page, size);
    }
    @GetMapping("/users/{id}") public UserView user(@PathVariable long id) { return service.user(id); }
    @PostMapping("/users") @ResponseStatus(HttpStatus.CREATED)
    public UserView createUser(@Valid @RequestBody NewUserInput input, Principal actor) { return service.createUser(input, actor.getName()); }
    @PutMapping("/users/{id}") public UserView updateUser(@PathVariable long id, @Valid @RequestBody UserInput input, Principal actor) { return service.updateUser(id, input, actor.getName()); }
    @PutMapping("/users/{id}/password") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void password(@PathVariable long id, @Valid @RequestBody PasswordInput input) { service.password(id, input.password); }

    @GetMapping("/options/clients") public List<Option> clientOptions(@RequestParam(defaultValue = "") String keyword) { return service.clientOptions(keyword); }
    @GetMapping("/options/licenses") public List<Option> licenseOptions(@RequestParam(defaultValue = "") String keyword) { return service.licenseOptions(keyword); }
}
