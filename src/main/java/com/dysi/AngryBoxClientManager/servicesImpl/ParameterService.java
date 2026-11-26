package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.AdminViews.Option;
import com.dysi.AngryBoxClientManager.dtos.ParameterInputs.*;
import com.dysi.AngryBoxClientManager.dtos.ParameterViews.*;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ParameterService {
    private final BankRepository banks;
    private final BankAccountRepository accounts;

    public ParameterService(BankRepository banks, BankAccountRepository accounts) {
        this.banks = banks; this.accounts = accounts;
    }

    public Page<BankView> banks(String keyword, String status, int page, int size) {
        Pageable paging = paging(keyword, status, page, size, "name");
        return banks.findAll((Specification<Bank>) (root, query, cb) -> cb.and(
                cb.isNull(root.get("removeDate")), like(cb, root.get("name"), keyword),
                state(cb, root.get("active"), status)), paging).map(this::view);
    }

    public BankView bank(long id) { return view(findBank(id)); }

    @Transactional
    public BankView saveBank(Long id, BankInput input) {
        Bank bank = id == null ? new Bank() : findBank(id);
        Bank normalized = new Bank(); normalized.setName(input.name);
        banks.findByNormalizedName(normalized.getNormalizedName()).ifPresent(other -> {
            if (!Objects.equals(other.getId(), id)) conflict("Ya existe un banco con ese nombre.");
        });
        bank.setName(normalized.getName()); bank.setActive(input.active);
        return view(banks.saveAndFlush(bank));
    }

    @Transactional
    public BankView bankActive(long id, boolean active) {
        Bank bank = findBank(id); bank.setActive(active);
        return view(banks.saveAndFlush(bank));
    }

    public Page<BankAccountView> accounts(String keyword, Long bankId, String status, int page, int size) {
        Pageable paging = paging(keyword, status, page, size, "holder");
        return accounts.findAll((Specification<BankAccount>) (root, query, cb) -> {
            Join<BankAccount, Bank> bank = root.join("bank");
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.isNull(root.get("removeDate")));
            filters.add(state(cb, root.get("active"), status));
            filters.add(cb.or(like(cb, root.get("holder"), keyword), like(cb, root.get("cbu"), keyword),
                    like(cb, root.get("externalCode"), keyword), like(cb, root.get("alias"), keyword), like(cb, bank.get("name"), keyword)));
            if (bankId != null) filters.add(cb.equal(bank.get("id"), bankId));
            return cb.and(filters.toArray(new Predicate[0]));
        }, paging).map(this::view);
    }

    public BankAccountView account(long id) { return view(findAccount(id)); }

    @Transactional
    public BankAccountView saveAccount(Long id, BankAccountInput input) {
        BankAccount account = id == null ? new BankAccount() : findAccount(id);
        Bank bank = findBank(input.bankId);
        BankAccount normalized = new BankAccount();
        normalized.setHolder(input.holder); normalized.setCbu(input.cbu); normalized.setAlias(input.alias);
        accounts.findByCbu(normalized.getCbu()).ifPresent(other -> {
            if (!Objects.equals(other.getId(), id)) conflict("Ya existe una cuenta con ese CBU, incluso si está inactiva.");
        });
        if (normalized.getAlias() != null) accounts.findByAlias(normalized.getAlias()).ifPresent(other -> {
            if (!Objects.equals(other.getId(), id)) conflict("Ya existe una cuenta con ese alias, incluso si está inactiva.");
        });
        // Omitido o null conserva el código; una cadena vacía lo limpia.
        if (input.externalCode != null) account.setExternalCode(input.externalCode);
        account.setBank(bank); account.setHolder(normalized.getHolder());
        account.setCbu(normalized.getCbu()); account.setAlias(normalized.getAlias()); account.setActive(input.active);
        return view(accounts.saveAndFlush(account));
    }

    @Transactional
    public BankAccountView accountActive(long id, boolean active) {
        BankAccount account = findAccount(id); account.setActive(active);
        return view(accounts.saveAndFlush(account));
    }

    public List<Option> bankOptions(String keyword) {
        return banks.findAll((Specification<Bank>) (root, query, cb) -> cb.and(
                cb.isNull(root.get("removeDate")), like(cb, root.get("name"), keyword)),
                paging(keyword, "ALL", 0, 20, "name")).stream()
                .map(bank -> new Option(bank.getId(), bank.getName() + (bank.getActive() ? "" : " (inactivo)")))
                .collect(Collectors.toList());
    }

    private Bank findBank(long id) {
        return banks.findById(id).filter(bank -> bank.getRemoveDate() == null)
                .orElseThrow(() -> missing("banco"));
    }
    private BankAccount findAccount(long id) {
        return accounts.findById(id).filter(account -> account.getRemoveDate() == null)
                .orElseThrow(() -> missing("cuenta bancaria"));
    }
    private BankView view(Bank bank) {
        BankView view = new BankView(); view.id = bank.getId(); view.name = bank.getName();
        view.active = bank.getActive(); return view;
    }
    private BankAccountView view(BankAccount account) {
        BankAccountView view = new BankAccountView(); view.id = account.getId(); view.bankId = account.getBank().getId();
        view.bankName = account.getBank().getName(); view.holder = account.getHolder();
        view.externalCode = account.getExternalCode();
        view.cbu = account.getCbu(); view.alias = account.getAlias(); view.active = account.getActive();
        view.bankActive = account.getBank().getActive() && account.getBank().getRemoveDate() == null;
        view.available = view.active && view.bankActive && account.getRemoveDate() == null; return view;
    }
    private static Predicate state(CriteriaBuilder cb, Path<Boolean> active, String status) {
        return "ALL".equals(status) ? cb.conjunction() : cb.equal(active, "ACTIVE".equals(status));
    }
    private static Predicate like(CriteriaBuilder cb, Expression<String> field, String keyword) {
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return cb.like(cb.lower(cb.coalesce(field, "")), pattern, '!');
    }
    private static Pageable paging(String keyword, String status, int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size || keyword.length() > 150
                || !Set.of("ALL", "ACTIVE", "INACTIVE").contains(status))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Parámetros de búsqueda inválidos.");
        return PageRequest.of(page, size, Sort.by(sort, "id"));
    }
    private static ResponseStatusException missing(String name) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el registro de " + name + ".");
    }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
