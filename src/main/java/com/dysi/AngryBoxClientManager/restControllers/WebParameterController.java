package com.dysi.AngryBoxClientManager.restControllers;

import com.dysi.AngryBoxClientManager.dtos.AdminViews.Option;
import com.dysi.AngryBoxClientManager.dtos.ParameterInputs.*;
import com.dysi.AngryBoxClientManager.dtos.ParameterViews.*;
import com.dysi.AngryBoxClientManager.servicesImpl.ParameterService;
import java.util.List;
import javax.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/web/api")
@PreAuthorize("hasRole('ADMIN')")
public class WebParameterController {
    private final ParameterService service;
    public WebParameterController(ParameterService service) { this.service = service; }

    @GetMapping("/banks")
    public Page<BankView> banks(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "ALL") String status, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) { return service.banks(keyword, status, page, size); }
    @GetMapping("/banks/{id}") public BankView bank(@PathVariable long id) { return service.bank(id); }
    @PostMapping("/banks") @ResponseStatus(HttpStatus.CREATED)
    public BankView createBank(@Valid @RequestBody BankInput input) { return service.saveBank(null, input); }
    @PutMapping("/banks/{id}")
    public BankView updateBank(@PathVariable long id, @Valid @RequestBody BankInput input) { return service.saveBank(id, input); }
    @PutMapping("/banks/{id}/active")
    public BankView bankActive(@PathVariable long id, @Valid @RequestBody ActiveInput input) { return service.bankActive(id, input.active); }

    @GetMapping("/bank-accounts")
    public Page<BankAccountView> accounts(@RequestParam(defaultValue = "") String keyword,
            @RequestParam(required = false) Long bankId, @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "50") int size) {
        return service.accounts(keyword, bankId, status, page, size);
    }
    @GetMapping("/bank-accounts/{id}") public BankAccountView account(@PathVariable long id) { return service.account(id); }
    @PostMapping("/bank-accounts") @ResponseStatus(HttpStatus.CREATED)
    public BankAccountView createAccount(@Valid @RequestBody BankAccountInput input) { return service.saveAccount(null, input); }
    @PutMapping("/bank-accounts/{id}")
    public BankAccountView updateAccount(@PathVariable long id, @Valid @RequestBody BankAccountInput input) { return service.saveAccount(id, input); }
    @PutMapping("/bank-accounts/{id}/active")
    public BankAccountView accountActive(@PathVariable long id, @Valid @RequestBody ActiveInput input) { return service.accountActive(id, input.active); }
    @GetMapping("/options/banks") public List<Option> bankOptions(@RequestParam(defaultValue = "") String keyword) { return service.bankOptions(keyword); }
}
