package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.AdminInputs.*;
import com.dysi.AngryBoxClientManager.dtos.AdminViews.*;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.criteria.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class WebAdminService {
    private final ClientRepository clients;
    private final LicenseRepository licenses;
    private final PaymentRepository payments;
    private final UserRepository users;
    private final AuthorityRepository authorities;
    private final PasswordEncoder passwords;

    public WebAdminService(ClientRepository clients, LicenseRepository licenses, PaymentRepository payments,
            UserRepository users, AuthorityRepository authorities, PasswordEncoder passwords) {
        this.clients = clients; this.licenses = licenses; this.payments = payments;
        this.users = users; this.authorities = authorities; this.passwords = passwords;
    }

    public Page<LicenseView> licenses(String keyword, Long clientId, String status, int page, int size) {
        Pageable paging = page(page, size, keyword, "companyName", "id");
        if (!Set.of("ALL", "ACTIVE", "EXPIRING", "EXPIRED", "BLOCKED").contains(status)) bad("Estado de licencia inválido.");
        Specification<License> specification = (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            Join<License, Client> client = root.join("client", JoinType.LEFT);
            filters.add(cb.or(like(cb, root.get("companyName"), keyword), like(cb, root.get("description"), keyword),
                    like(cb, cb.concat(cb.concat(client.get("firstname"), " "), client.get("lastname")), keyword),
                    like(cb, client.get("legalName"), keyword), like(cb, client.get("tradeName"), keyword)));
            if (clientId != null) filters.add(cb.equal(client.get("id"), clientId));
            if (!"ALL".equals(status)) {
                Join<License, LicenseStatus> state = root.join("licenseStatuses");
                Subquery<Long> first = query.subquery(Long.class);
                Root<LicenseStatus> candidate = first.from(LicenseStatus.class);
                first.select(cb.min(candidate.<Long>get("id"))).where(cb.equal(candidate.get("license"), root), cb.isNull(candidate.get("removeDate")));
                filters.add(cb.equal(state.get("id"), first));
                if ("BLOCKED".equals(status)) filters.add(cb.isTrue(state.get("forcedLock")));
                else {
                    filters.add(cb.isFalse(state.get("forcedLock")));
                    Calendar today = calendar(LocalDate.now());
                    if ("EXPIRED".equals(status)) filters.add(cb.lessThanOrEqualTo(state.<Calendar>get("lockDate"), today));
                    else {
                        filters.add(cb.greaterThan(state.<Calendar>get("lockDate"), today));
                        if ("EXPIRING".equals(status)) filters.add(cb.lessThanOrEqualTo(state.<Calendar>get("lockDate"), calendar(LocalDate.now().plusDays(15))));
                    }
                }
            }
            return cb.and(filters.toArray(new Predicate[0]));
        };
        return licenses.findAll(specification, paging).map(WebAdminService::licenseView);
    }

    public LicenseView license(long id) { return licenseView(findLicense(id)); }

    @Transactional
    public LicenseView saveLicense(Long id, LicenseInput input) {
        License license = id == null ? new License() : findLicense(id);
        if (licenses.findLicensesByCompanyId(input.companyId).stream().anyMatch(other -> !other.getId().equals(id)))
            conflict("El identificador de empresa ya está en uso.");
        Client client = findClient(input.clientId);
        if (id != null && !Objects.equals(license.getClient().getId(), input.clientId)) bad("Una licencia existente no puede cambiar de cliente.");
        license.setClient(client); license.setCompanyId(input.companyId); license.setCompanyName(input.companyName.trim());
        license.setDescription(input.description.trim()); license.setInitialActivity(calendar(input.initialActivity));
        license.setPrePayment(input.prePayment);
        if (license.getLicenseStatuses() == null) license.setLicenseStatuses(new ArrayList<>());
        LicenseStatus state = currentState(license);
        if (state == null) {
            state = new LicenseStatus(); state.setLicense(license);
            state.setForcedLock(false); state.setLimitTransaction(9999999L); state.setObservation("");
            license.getLicenseStatuses().add(state);
        }
        state.setLockDate(calendar(input.lockDate));
        return licenseView(licenses.saveAndFlush(license));
    }

    @Transactional
    public LicenseView lockLicense(long id, boolean locked) {
        License license = findLicense(id);
        LicenseStatus state = currentState(license);
        if (state == null) conflict("La licencia no tiene estado. Completá sus datos antes de bloquearla.");
        state.setForcedLock(locked);
        licenses.saveAndFlush(license);
        return licenseView(license);
    }

    public Page<PaymentView> payments(String keyword, Long licenseId, String status, int page, int size) {
        return payments(keyword, licenseId, status, null, null, null, page, size);
    }

    public Page<PaymentView> payments(String keyword, Long licenseId, String status, Long clientId,
            LocalDate from, LocalDate to, int page, int size) {
        page(page, size, keyword, "issuedAt", "id");
        Set<PaymentStatus> selected = paymentStatuses(status);
        Specification<Payment> specification = paymentFilters(keyword, licenseId, clientId, from, to);
        if (!selected.isEmpty()) specification = specification.and((root, query, cb) -> root.get("status").in(selected));
        return payments.findAll(specification, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "issuedAt", "id"))).map(WebAdminService::paymentView);
    }

    /** Los contadores conservan los filtros de contexto, sin restringirse a los estados seleccionados. */
    public PaymentCounts paymentCounts(String keyword, Long licenseId, Long clientId, LocalDate from, LocalDate to) {
        page(0, 50, keyword, "id");
        Specification<Payment> specification = paymentFilters(keyword, licenseId, clientId, from, to);
        PaymentCounts counts = new PaymentCounts();
        counts.pending = countPayments(specification, PaymentStatus.PENDIENTE);
        counts.approved = countPayments(specification, PaymentStatus.APROBADO);
        counts.rejected = countPayments(specification, PaymentStatus.RECHAZADO);
        counts.total = counts.pending + counts.approved + counts.rejected;
        return counts;
    }

    private long countPayments(Specification<Payment> specification, PaymentStatus status) {
        return payments.count(specification.and((root, query, cb) -> cb.equal(root.get("status"), status)));
    }

    private static Set<PaymentStatus> paymentStatuses(String status) {
        Set<PaymentStatus> selected = EnumSet.noneOf(PaymentStatus.class);
        if (status == null || status.isEmpty() || "ALL".equals(status)) return selected;
        for (String value : status.split(",", -1)) {
            try { selected.add(PaymentStatus.valueOf(value)); }
            catch (IllegalArgumentException invalid) { bad("Estado de pago inválido."); }
        }
        return selected;
    }

    private Specification<Payment> paymentFilters(String keyword, Long licenseId, Long clientId, LocalDate from, LocalDate to) {
        if (from != null && to != null && from.isAfter(to)) bad("La fecha desde no puede ser posterior a hasta.");
        if ((from != null && (from.getYear() < 1000 || from.getYear() > 9999))
                || (to != null && (to.getYear() < 1000 || to.getYear() > 9998))) bad("Rango de fechas inválido.");
        return (root, query, cb) -> {
            Join<Payment, License> license = root.join("license", JoinType.LEFT);
            Join<Payment, Client> client = root.join("client");
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.or(like(cb, license.get("companyName"), keyword),
                    like(cb, cb.concat(cb.concat(client.get("firstname"), " "), client.get("lastname")), keyword),
                    like(cb, client.get("legalName"), keyword), like(cb, client.get("tradeName"), keyword)));
            filters.add(cb.isNull(root.get("removeDate")));
            if (clientId != null) filters.add(cb.equal(client.get("id"), clientId));
            if (from != null) filters.add(cb.greaterThanOrEqualTo(root.get("issuedAt"), from.atStartOfDay()));
            if (to != null) filters.add(cb.lessThan(root.get("issuedAt"), to.plusDays(1).atStartOfDay()));
            if (licenseId != null) filters.add(cb.equal(license.get("id"), licenseId));
            return cb.and(filters.toArray(new Predicate[0]));
        };
    }

    public PaymentView payment(long id) { return paymentView(findPayment(id)); }

    /** El formulario anterior no tiene los datos ni las evidencias del nuevo contrato. */
    @Transactional
    public PaymentView savePayment(Long id, PaymentInput input) {
        if (id != null) findPayment(id);
        throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Usá el flujo de presentación de pagos. Esta operación no admite edición genérica.");
    }

    @Transactional
    public PaymentView paymentStatus(long id, boolean removed) {
        findPayment(id);
        throw new ResponseStatusException(HttpStatus.CONFLICT, "Los pagos no admiten anulación ni reactivación.");
    }

    public Page<UserView> users(String keyword, Long clientId, Boolean enabled, AuthorityName authority, int page, int size) {
        return users.findAll((Specification<User>) (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.or(like(cb, root.get("username"), keyword), like(cb, root.get("email"), keyword),
                    like(cb, cb.concat(cb.concat(root.get("firstname"), " "), root.get("lastname")), keyword)));
            if (clientId != null) filters.add(cb.equal(root.get("client").get("id"), clientId));
            if (enabled != null) filters.add(cb.equal(root.get("enabled"), enabled));
            if (authority != null) {
                query.distinct(true);
                filters.add(cb.equal(root.join("authorities").get("name"), authority));
            }
            return cb.and(filters.toArray(new Predicate[0]));
        }, page(page, size, keyword, "username", "id")).map(this::userView);
    }

    public UserView user(long id) { return userView(findUser(id)); }

    @Transactional
    public UserView createUser(NewUserInput input, String actor) {
        if (users.findByUsername(input.username.trim()) != null) conflict("El nombre de usuario ya existe.");
        checkPassword(input.password);
        User user = new User(); user.setUsername(input.username.trim()); user.setPassword(passwords.encode(input.password));
        user.setLastPasswordResetDate(new Date());
        fillUser(user, input, actor);
        return userView(users.saveAndFlush(user));
    }

    @Transactional
    public UserView updateUser(long id, UserInput input, String actor) {
        User user = findUser(id);
        if (!user.getUsername().equals(input.username.trim())) bad("El nombre de usuario no se puede modificar.");
        fillUser(user, input, actor);
        return userView(users.saveAndFlush(user));
    }

    private void fillUser(User user, UserInput input, String actor) {
        if (input.username.trim().length() < 4 || input.firstname.trim().length() < 4 || input.lastname.trim().length() < 4)
            bad("Usuario, nombre y apellido deben tener al menos cuatro caracteres sin espacios exteriores.");
        if (user.getUsername().equals(actor) && (!input.enabled || !input.roles.contains(AuthorityName.ROLE_ADMIN)))
            conflict("No podés deshabilitar tu propia cuenta ni quitarte el rol de administrador.");
        List<Authority> roles = new ArrayList<>();
        List<Authority> configuredRoles = input.roles.contains(AuthorityName.ROLE_API)
                ? authorities.findAllForAssignment() : authorities.findAll();
        for (AuthorityName role : new LinkedHashSet<>(input.roles)) {
            Authority authority = configuredRoles.stream().filter(a -> a.getName() == role).findFirst()
                    .orElseGet(() -> {
                        if (role == AuthorityName.ROLE_API) {
                            // Alta explícita al asignar el permiso. JPA conserva el generador de IDs de la instancia.
                            Authority api = new Authority();
                            api.setName(AuthorityName.ROLE_API);
                            api = authorities.saveAndFlush(api);
                            configuredRoles.add(api);
                            return api;
                        }
                        return null;
                    });
            if (authority == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "El rol " + role + " no está configurado en la base.");
            }
            roles.add(authority);
        }
        Client previous = user.getClient();
        Client next = input.clientId == null ? null : findClient(input.clientId);
        if (next != null && ((next.getUser() != null && !Objects.equals(next.getUser().getId(), user.getId()))
                || users.findByClient(next).stream().anyMatch(other -> !Objects.equals(other.getId(), user.getId()))))
            conflict("El cliente ya tiene un usuario vinculado.");
        if (previous != null && previous != next && previous.getUser() != null && Objects.equals(previous.getUser().getId(), user.getId())) {
            previous.setUser(null); clients.save(previous);
        }
        user.setFirstname(input.firstname.trim()); user.setLastname(input.lastname.trim()); user.setEmail(input.email.trim());
        user.setEnabled(input.enabled); user.setAuthorities(roles); user.setClient(next);
        users.saveAndFlush(user);
        if (next != null) { next.setUser(user); clients.saveAndFlush(next); }
    }

    @Transactional
    public void password(long id, String password) {
        checkPassword(password);
        User user = findUser(id);
        user.setPassword(passwords.encode(password)); user.setLastPasswordResetDate(new Date()); users.saveAndFlush(user);
    }

    public List<Option> clientOptions(String keyword) {
        return clients.searchForWeb(pattern(keyword), page(0, 20, keyword, "lastname", "id")).getContent().stream()
                .map(c -> new Option(c.getId(), clientOptionLabel(c))).collect(Collectors.toList());
    }
    private static String clientOptionLabel(Client client) {
        List<String> names = new ArrayList<>();
        names.add(clientName(client));
        if (client.getTradeName() != null) names.add(client.getTradeName());
        if (client.getLegalName() != null) names.add(client.getLegalName());
        return String.join(" · ", names);
    }
    public List<Option> licenseOptions(String keyword) {
        return licenses(keyword, null, "ALL", 0, 20).getContent().stream()
                .map(l -> new Option(l.id, l.companyName + " · Empresa #" + l.companyId)).collect(Collectors.toList());
    }

    static LicenseView licenseView(License license) {
        LicenseView view = new LicenseView();
        view.id = license.getId(); view.companyId = license.getCompanyId(); view.companyName = license.getCompanyName();
        view.description = license.getDescription(); view.clientId = license.getClient() == null ? null : license.getClient().getId();
        view.clientName = clientName(license.getClient()); view.initialActivity = date(license.getInitialActivity()); view.prePayment = license.isPrePayment();
        LicenseStatus state = currentState(license);
        view.lockDate = state == null ? null : date(state.getLockDate()); view.locked = state != null && state.isForcedLock();
        // Calendar days in the server's business timezone, shared by status and countdown.
        view.daysUntilLock = view.lockDate == null ? null : java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), view.lockDate);
        view.status = view.locked ? "BLOCKED" : view.daysUntilLock == null ? "UNKNOWN" : view.daysUntilLock <= 0 ? "EXPIRED"
                : view.daysUntilLock <= 15 ? "EXPIRING" : "ACTIVE";
        return view;
    }
    public static PaymentView paymentView(Payment payment) {
        PaymentView view = new PaymentView(); view.id = payment.getId();
        view.clientId = payment.getClient().getId(); view.clientName = clientName(payment.getClient());
        view.licenseId = payment.getLicense() == null ? null : payment.getLicense().getId();
        view.companyName = payment.getLicense() == null ? null : payment.getLicense().getCompanyName();
        view.amount = payment.getAmount(); view.currency = payment.getCurrency().name();
        view.issuedAt = payment.getIssuedAt(); view.status = payment.getStatus().name();
        return view;
    }
    private UserView userView(User user) {
        UserView view = new UserView(); view.id = user.getId(); view.username = user.getUsername(); view.firstname = user.getFirstname();
        view.lastname = user.getLastname(); view.email = user.getEmail(); view.enabled = Boolean.TRUE.equals(user.getEnabled());
        view.roles = user.getAuthorities().stream().map(Authority::getName).collect(Collectors.toList());
        view.clientId = user.getClient() == null ? null : user.getClient().getId(); view.clientName = clientName(user.getClient());
        return view;
    }
    private static LicenseStatus currentState(License license) {
        return license.getLicenseStatuses() == null ? null : license.getLicenseStatuses().stream().filter(s -> s.getRemoveDate() == null)
                .min(Comparator.comparing(LicenseStatus::getId, Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null);
    }
    private License findLicense(long id) { return licenses.findById(id).orElseThrow(() -> missing("licencia")); }
    private Payment findPayment(long id) { return payments.findById(id).orElseThrow(() -> missing("pago")); }
    private Client findClient(long id) { return clients.findById(id).orElseThrow(() -> missing("cliente")); }
    private User findUser(long id) { return users.findById(id).orElseThrow(() -> missing("usuario")); }
    private ResponseStatusException missing(String entity) { return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el registro de " + entity + "."); }
    private static String clientName(Client client) { return client == null ? "Sin cliente" : client.getFirstname() + " " + client.getLastname(); }
    private static Calendar calendar(LocalDate value) { return value == null ? null : GregorianCalendar.from(value.atStartOfDay(ZoneId.systemDefault())); }
    private static LocalDate date(Calendar value) { return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); }
    private static String pattern(String keyword) { return "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%"; }
    private static Predicate like(CriteriaBuilder cb, Expression<String> field, String keyword) { return cb.like(cb.lower(cb.coalesce(field, "")), pattern(keyword), '!'); }
    private static Pageable page(int page, int size, String keyword, String... sort) {
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size || keyword.length() > 150) bad("Parámetros de búsqueda inválidos.");
        return PageRequest.of(page, size, Sort.by(sort));
    }
    private static void checkPassword(String password) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            bad("La contraseña debe tener al menos 12 caracteres y no superar 72 bytes UTF-8.");
    }
    private static void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private static void conflict(String message) { throw new ResponseStatusException(HttpStatus.CONFLICT, message); }
}
