package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.PaymentReviewInputs;
import com.dysi.AngryBoxClientManager.dtos.PaymentReviewViews;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.dysi.AngryBoxClientManager.security.JwtUser;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;
import javax.persistence.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Revisión exclusiva de ADMIN. No modifica saldos ni expone este contrato al portal. */
@Service
@PreAuthorize("hasRole('ADMIN')")
@Transactional(readOnly = true)
public class PaymentReviewService {
    // Una licencia oculta solo se excluye cuando su estado histórico vigente tiene bloqueo forzado.
    private static final String APPROVAL_LICENSES = " from License l where l.client.id = :clientId and l.removeDate is null "
            + "and not (l.hidden = true and exists (select s.id from LicenseStatus s where s.license = l "
            + "and s.removeDate is null and s.forcedLock = true and s.id = "
            + "(select min(t.id) from LicenseStatus t where t.license = l and t.removeDate is null)))";
    private final EntityManager em;
    private final PaymentRepository payments;
    private final LicenseRepository licenses;
    private final ClientRepository clients;
    private final ZoneId businessZone;
    private final LicenseExtensionService extensions;
    public PaymentReviewService(EntityManager em, PaymentRepository payments, LicenseRepository licenses,
            ClientRepository clients, @Value("${portal.review.time-zone:America/Argentina/Buenos_Aires}") String timeZone, LicenseExtensionService extensions) {
        this.extensions = extensions;
        this.em = em; this.payments = payments; this.licenses = licenses; this.clients = clients;
        this.businessZone = ZoneId.of(timeZone);
    }

    public PaymentReviewViews.Detail detail(Long id) { actor(); return detailView(payment(id)); }

    public PaymentReviewViews.ApprovalLicenses approvalLicenses(Long id) {
        actor(); Payment payment = payment(id);
        List<License> found = em.createQuery("select l" + APPROVAL_LICENSES + " order by l.id", License.class)
                .setParameter("clientId", payment.getClient().getId()).setMaxResults(101).getResultList();
        checkApprovalSize(found.size());
        Map<Long, LicenseStatus> states = licenseStates(found.stream().map(License::getId).collect(Collectors.toList()));
        PaymentReviewViews.ApprovalLicenses view = new PaymentReviewViews.ApprovalLicenses();
        view.today = LocalDate.now(businessZone); view.suggestedDate = suggestedDate(view.today, null);
        view.licenses = found.stream().map(license -> option(license, states.get(license.getId()))).collect(Collectors.toList());
        return view;
    }

    private void checkApprovalSize(int count) {
        if (count > 100) bad("El cliente tiene más de 100 licencias para revisar. El límite actual de aprobación es 100; no se aprobó el pago ni se omitieron licencias.");
    }

    public List<PaymentReviewViews.BankDestination> bankAccounts(Long id) {
        actor(); payment(id);
        return em.createQuery("select a from BankAccount a join fetch a.bank b where a.removeDate is null "
                + "and a.active = true and b.removeDate is null and b.active = true order by b.name, a.id", BankAccount.class)
                .getResultList().stream().map(account -> {
                    PaymentReviewViews.BankDestination view = new PaymentReviewViews.BankDestination();
                    view.id = account.getId(); view.bank = account.getBank().getName(); view.holder = account.getHolder();
                    view.cbu = account.getCbu(); view.alias = account.getAlias(); view.revision = BankAccountRevision.of(account);
                    return view;
                }).collect(Collectors.toList());
    }

    /** Corrección previa a la decisión, atómica con su evidencia privada e independiente de la aprobación. */
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED, timeout = 120)
    public PaymentReviewViews.Detail correct(Long id, PaymentReviewInputs.Correction input) {
        User actor = actor(); Payment payment = lockPayment(id); pending(payment, input.version);
        if (input.issuedAt.getYear() < 1000 || input.issuedAt.getYear() > 9999) bad("La fecha de emisión no es válida.");
        License license = input.licenseId == null ? null : em.find(License.class, input.licenseId, LockModeType.PESSIMISTIC_WRITE);
        if (license != null) em.refresh(license);
        if (input.licenseId != null && (license == null || license.getRemoveDate() != null
                || license.getClient() == null || !license.getClient().getId().equals(payment.getClient().getId())))
            bad("La licencia no debe estar anulada y debe pertenecer al cliente del pago.");
        String previousHash = PaymentSubmissionFingerprint.of(payment);
        BankAccount account = null;
        if (input.bankAccountId != null) {
            account = em.find(BankAccount.class, input.bankAccountId, LockModeType.PESSIMISTIC_READ);
            if (account == null) bad("La cuenta destinataria no existe.");
            em.refresh(account);
            Bank bank = em.find(Bank.class, account.getBank().getId(), LockModeType.PESSIMISTIC_READ);
            if (bank != null) em.refresh(bank);
            if (account.getRemoveDate() != null || !account.getActive() || bank == null || !bank.getActive() || bank.getRemoveDate() != null)
                conflict("La cuenta destinataria ya no está disponible. Actualizá las cuentas y elegí otra.");
            if (!BankAccountRevision.of(account).equals(input.accountRevision))
                conflict("Los datos bancarios cambiaron. Actualizá las cuentas y revisá el destino antes de guardar.");
        } else if (input.accountRevision != null) bad("Elegí una cuenta para cambiar el destino bancario.");
        boolean destinationChanged = account != null && !BankAccountRevision.of(payment).equals(BankAccountRevision.of(account));
        if (!destinationChanged && previousHash.equals(PaymentSubmissionFingerprint.of(input.licenseId, input.amount, input.issuedAt, input.contactPhone)))
            bad("Modificá al menos un dato del pago antes de guardar.");
        String previous = correctionValues(payment);
        payment.preserveOriginalEditableHash(previousHash);
        if (destinationChanged) {
            payment.preserveOriginalDestinationHash(BankAccountRevision.submissionHash(payment));
            payment.setBankAccount(account); payment.setDestinationBank(account.getBank().getName());
            payment.setDestinationHolder(account.getHolder()); payment.setDestinationCbu(account.getCbu());
            payment.setDestinationAlias(account.getAlias());
        }
        payment.setAmount(input.amount); payment.setIssuedAt(input.issuedAt.truncatedTo(ChronoUnit.MICROS));
        payment.setContactPhone(input.contactPhone.trim()); payment.setLicense(license);
        Comment comment = new Comment(); comment.setVisibility(CommentVisibility.PRIVADO);
        comment.setText("Corrección de pago pendiente. Motivo: " + input.reason.trim()
                + "\nAntes: " + previous + "\nDespués: " + correctionValues(payment));
        comment.setAuthor(actor); comment.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); comment.setOccurredAt(now()); em.persist(comment);
        PaymentComment link = new PaymentComment(); link.setPayment(payment); link.setComment(comment); em.persist(link);
        em.flush(); return detailView(payment);
    }

    private String correctionValues(Payment payment) {
        return "importe ARS " + payment.getAmount().toPlainString() + "; emisión " + payment.getIssuedAt()
                + "; teléfono " + payment.getContactPhone() + "; licencia "
                + (payment.getLicense() == null ? "sin asociar" : "#" + payment.getLicense().getId())
                + "; cuenta #" + payment.getBankAccount().getId() + "; banco " + payment.getDestinationBank()
                + "; titular " + payment.getDestinationHolder() + "; CBU " + payment.getDestinationCbu()
                + "; alias " + Objects.toString(payment.getDestinationAlias(), "sin alias");
    }

    public Page<PaymentReviewViews.Comment> comments(Long id, int page, int size) {
        actor(); payment(id);
        return entities("pc", "PaymentComment pc join fetch pc.comment c left join fetch c.author", "PaymentComment pc join pc.comment c",
                "pc.payment.id = :id and pc.removeDate is null and c.removeDate is null", PaymentComment.class, id, page, size).map(link -> commentView(link.getComment()));
    }
    public Page<PaymentReviewViews.Receipt> receipts(Long id, int page, int size) {
        actor(); payment(id);
        return entities("pa", "PaymentAttachment pa join fetch pa.attachment a left join fetch a.uploadedBy", "PaymentAttachment pa join pa.attachment a",
                "pa.payment.id = :id and pa.removeDate is null and a.removeDate is null", PaymentAttachment.class, id, page, size).map(link -> {
            Attachment file = link.getAttachment(); PaymentReviewViews.Receipt view = new PaymentReviewViews.Receipt();
            view.id = file.getId(); view.originalName = file.getOriginalName(); view.description = file.getDescription();
            view.mimeType = file.getMimeType(); view.size = file.getStoredSize(); view.uploadedAt = file.getUploadedAt();
            view.uploadedBy = actorView(file.getUploadedBy()); view.origin = file.getOrigin().name(); return view;
        });
    }
    public Page<PaymentReviewViews.History> history(Long id, int page, int size) {
        actor(); payment(id);
        return entities("h", "PaymentStatusHistory h left join fetch h.responsibleUser", "PaymentStatusHistory h",
                "h.payment.id = :id", PaymentStatusHistory.class, id, page, size).map(event -> {
            PaymentReviewViews.History view = new PaymentReviewViews.History(); view.id = event.getId();
            view.previousStatus = event.getPreviousStatus() == null ? null : event.getPreviousStatus().name();
            view.newStatus = event.getNewStatus().name(); view.reason = event.getReason(); view.occurredAt = event.getOccurredAt();
            view.origin = event.getOrigin().name(); view.responsible = actorView(event.getResponsibleUser()); return view;
        });
    }
    public Page<PaymentReviewViews.LicenseEvent> licenseEvents(Long id, int page, int size) {
        actor(); payment(id);
        return entities("a", "PaymentLicenseAction a join fetch a.license left join fetch a.responsibleUser", "PaymentLicenseAction a",
                "a.payment.id = :id", PaymentLicenseAction.class, id, page, size).map(event -> {
            PaymentReviewViews.LicenseEvent view = new PaymentReviewViews.LicenseEvent(); view.id = event.getId();
            view.licenseId = event.getLicense().getId(); view.companyName = event.getLicense().getCompanyName();
            view.actionType = event.getActionType().name(); view.previousValue = event.getPreviousValue(); view.newValue = event.getNewValue();
            view.occurredAt = event.getOccurredAt(); view.responsible = actorView(event.getResponsibleUser()); return view;
        });
    }
    public Page<PaymentReviewViews.LicenseOption> licenses(Long id, String keyword, int page, int size) {
        actor(); Payment payment = payment(id); Pageable pagination = page(page, size);
        if (keyword == null || keyword.length() > 150) bad("Búsqueda inválida.");
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        Page<License> result = licenses.findAll((Specification<License>) (root, query, cb) -> cb.and(
                cb.equal(root.get("client").get("id"), payment.getClient().getId()), cb.isNull(root.get("removeDate")),
                cb.like(cb.lower(cb.coalesce(root.get("companyName"), "")), pattern, '!')), pagination);
        List<Long> ids = result.stream().map(License::getId).collect(Collectors.toList());
        Map<Long, LicenseStatus> states = licenseStates(ids);
        return result.map(license -> option(license, states.get(license.getId())));
    }

    private Map<Long, LicenseStatus> licenseStates(List<Long> ids) {
        Map<Long, LicenseStatus> states = new HashMap<>();
        if (!ids.isEmpty()) for (LicenseStatus state : em.createQuery("select s from LicenseStatus s where s.license.id in :ids "
                + "and s.removeDate is null and s.id = (select min(t.id) from LicenseStatus t where t.license = s.license and t.removeDate is null)", LicenseStatus.class)
                .setParameter("ids", ids).getResultList()) states.put(state.getLicense().getId(), state);
        return states;
    }

    @Transactional
    public PaymentReviewViews.Comment comment(Long id, PaymentReviewInputs.CommentInput input) {
        User actor = actor(); Payment payment = payment(id);
        Comment comment = new Comment(); comment.setText(input.text.trim()); comment.setVisibility(input.visibility);
        comment.setAuthor(actor); comment.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); comment.setOccurredAt(now()); em.persist(comment);
        PaymentComment link = new PaymentComment(); link.setPayment(payment); link.setComment(comment); em.persist(link); em.flush();
        return commentView(comment);
    }
    public PaymentReviewViews.Preview preview(Long id, PaymentReviewInputs.Options input) {
        actor(); Payment payment = payment(id); pending(payment, input.version);
        return plan(payment, input, false).view;
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED, timeout = 120)
    public PaymentReviewViews.Detail approve(Long id, PaymentReviewInputs.Approval input) {
        User actor = actor(); Payment payment = lockPayment(id); pending(payment, input.version);
        Plan plan = plan(payment, input, true);
        if (!plan.view.today.equals(input.reviewedOn) || !plan.view.token.equals(input.previewToken))
            conflict("La revisión cambió o cambió el día. Actualizá la vista previa antes de confirmar.");
        for (Change change : plan.changes) if (change.view.shortening && !change.input.confirmShortening)
            bad("Confirmá expresamente cada reducción del vencimiento.");
        LocalDateTime now = now();
        payment.decide(PaymentStatus.APROBADO);
        extensions.approve(payment);
        // La aprobación y su entrega pendiente se confirman en la misma transacción.
        AngryboxPaymentDelivery delivery = new AngryboxPaymentDelivery();
        delivery.setPayment(payment);
        em.persist(delivery);
        for (Change change : plan.changes) apply(payment, change, actor, now);
        history(payment, PaymentStatus.APROBADO, null, actor, now); em.flush(); return detailView(payment);
    }
    @Transactional(isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED, timeout = 120)
    public PaymentReviewViews.Detail reject(Long id, PaymentReviewInputs.Rejection input) {
        User actor = actor(); Payment payment = lockPayment(id); pending(payment, input.version);
        payment.decide(PaymentStatus.RECHAZADO);
        extensions.reject(payment);
        history(payment, PaymentStatus.RECHAZADO, input.reason.trim(), actor, now()); em.flush(); return detailView(payment);
    }

    private Payment lockPayment(Long id) {
        Payment found = payment(id);
        // Orden compartido con el portal: cliente -> pago -> licencias ordenadas por ID.
        clients.lockPortalClient(found.getClient().getId()).orElseThrow(this::missing);
        Payment locked = em.find(Payment.class, id, LockModeType.PESSIMISTIC_WRITE); em.refresh(locked);
        if (locked.getRemoveDate() != null) throw missing(); return locked;
    }
    private Plan plan(Payment payment, PaymentReviewInputs.Options input, boolean lock) {
        Plan plan = new Plan(); plan.view = new PaymentReviewViews.Preview(); plan.view.today = LocalDate.now(businessZone);
        plan.view.licenses = new ArrayList<>(); plan.changes = new ArrayList<>();
        List<String> fingerprint = new ArrayList<>(List.of(payment.getId().toString(), Long.toString(payment.getVersion()),
                plan.view.today.toString(), Boolean.toString(input.reviewAllLicenses)));
        if (input.reviewAllLicenses) {
            List<Long> currentIds = em.createQuery("select l.id" + APPROVAL_LICENSES, Long.class)
                    .setParameter("clientId", payment.getClient().getId()).setMaxResults(101).getResultList();
            checkApprovalSize(currentIds.size());
            Set<Long> requestedIds = input.licenses.stream().map(action -> action.licenseId).collect(Collectors.toSet());
            if (requestedIds.size() != input.licenses.size() || !requestedIds.equals(new HashSet<>(currentIds)))
                conflict("Cambió el listado de licencias del cliente. Volvé a abrir la aprobación y revisá todas las licencias.");
        }
        Set<Long> seen = new HashSet<>();
        List<PaymentReviewInputs.LicenseAction> actions = new ArrayList<>(input.licenses);
        actions.sort(Comparator.comparing(action -> action.licenseId));
        for (PaymentReviewInputs.LicenseAction action : actions) {
            if (!seen.add(action.licenseId)) bad("No repitas una licencia en la aprobación.");
            if (!input.reviewAllLicenses && !action.unlock && !action.updateExpiry)
                bad("Elegí al menos una acción por licencia o quitá la licencia del envío.");
            License license = lock ? em.find(License.class, action.licenseId, LockModeType.PESSIMISTIC_WRITE) : em.find(License.class, action.licenseId);
            if (lock && license != null) em.refresh(license);
            if (license == null || license.getRemoveDate() != null || license.getClient() == null
                    || !license.getClient().getId().equals(payment.getClient().getId())) throw missing();
            LicenseStatus state = state(license);
            if (lock && state != null) { em.lock(state, LockModeType.PESSIMISTIC_WRITE); em.refresh(state); }
            if (state != null && state.getRemoveDate() != null) conflict("El estado de licencia cambió. Volvé a abrir la aprobación.");
            PaymentReviewViews.LicenseOption current = option(license, state);
            if (!current.revision.equals(action.revision)) conflict("La licencia cambió. Volvé a abrir la aprobación y revisá sus datos.");
            if (action.updateExpiry && (action.lockDate == null || action.lockDate.isBefore(plan.view.today)
                    || action.lockDate.getYear() > 9999)) bad("El vencimiento debe ser hoy o una fecha posterior válida.");
            if (!action.updateExpiry && action.lockDate != null) bad("No envíes una fecha sin elegir actualizar vencimiento.");
            PaymentReviewViews.LicensePreview view = new PaymentReviewViews.LicensePreview();
            copyOption(current, view); view.unlock = action.unlock; view.updateExpiry = action.updateExpiry;
            view.proposedDate = action.updateExpiry ? action.lockDate : current.lockDate;
            view.shortening = action.updateExpiry && current.lockDate != null && action.lockDate.isBefore(current.lockDate);
            view.daysFromToday = view.proposedDate == null ? null : ChronoUnit.DAYS.between(plan.view.today, view.proposedDate);
            view.differenceFromPrevious = current.lockDate == null || view.proposedDate == null ? null : ChronoUnit.DAYS.between(current.lockDate, view.proposedDate);
            view.resultingStatus = status(current.forcedLock && !action.unlock, view.proposedDate);
            Change change = new Change(); change.input = action; change.license = license; change.state = state; change.view = view;
            plan.changes.add(change); plan.view.licenses.add(view);
            fingerprint.addAll(Arrays.asList(current.id.toString(), current.revision, Boolean.toString(action.unlock), Boolean.toString(action.updateExpiry),
                    action.lockDate == null ? null : action.lockDate.toString()));
        }
        plan.view.token = ReviewFingerprint.of(fingerprint.toArray(new String[0])); return plan;
    }
    private void apply(Payment payment, Change change, User actor, LocalDateTime now) {
        LicenseStatus state = change.state;
        if (state == null && change.input.updateExpiry) {
            state = new LicenseStatus(); state.setLicense(change.license); state.setLimitTransaction(9999999L);
            state.setObservation(""); state.setForcedLock(false); em.persist(state);
        }
        if (state != null && change.input.unlock && state.isForcedLock()) {
            state.setForcedLock(false); licenseEvent(payment, change.license, LicenseActionType.DESBLOQUEO, "true", "false", actor, now);
        }
        if (change.input.updateExpiry && !Objects.equals(change.view.lockDate, change.input.lockDate)) {
            state.setLockDate(calendar(change.input.lockDate));
            licenseEvent(payment, change.license, LicenseActionType.VENCIMIENTO,
                    change.view.lockDate == null ? null : change.view.lockDate.toString(), change.input.lockDate.toString(), actor, now);
        }
    }
    private void history(Payment payment, PaymentStatus next, String reason, User actor, LocalDateTime now) {
        PaymentStatusHistory event = new PaymentStatusHistory(); event.setPayment(payment); event.setPreviousStatus(PaymentStatus.PENDIENTE);
        event.setNewStatus(next); event.setReason(reason); event.setOccurredAt(now); event.setResponsibleUser(actor);
        event.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); em.persist(event);
    }
    private void licenseEvent(Payment payment, License license, LicenseActionType type, String previous, String next, User actor, LocalDateTime now) {
        PaymentLicenseAction event = new PaymentLicenseAction(); event.setPayment(payment); event.setLicense(license); event.setActionType(type);
        event.setPreviousValue(previous); event.setNewValue(next); event.setResponsibleUser(actor); event.setOccurredAt(now); em.persist(event);
    }
    private PaymentReviewViews.Detail detailView(Payment payment) {
        PaymentReviewViews.Detail view = new PaymentReviewViews.Detail(); view.payment = WebAdminService.paymentView(payment); view.version = payment.getVersion();
        view.contactPhone = payment.getContactPhone(); view.method = payment.getMethod().name(); view.destinationBank = payment.getDestinationBank();
        view.destinationHolder = payment.getDestinationHolder(); view.destinationCbu = payment.getDestinationCbu(); view.destinationAlias = payment.getDestinationAlias();
        view.withoutReceipt = payment.getWithoutReceipt(); view.noReceiptReason = payment.getNoReceiptReason(); view.origin = payment.getOrigin().name();
        view.submittedBy = actorView(payment.getSubmittedBy()); view.createdAt = payment.getCreationDate() == null ? null
                : LocalDateTime.ofInstant(payment.getCreationDate().toInstant(), ZoneId.systemDefault());
        License license = payment.getLicense();
        if (license != null && license.getRemoveDate() == null) view.associatedLicense = option(license, state(license)); return view;
    }
    private PaymentReviewViews.LicenseOption option(License license, LicenseStatus state) {
        PaymentReviewViews.LicenseOption view = new PaymentReviewViews.LicenseOption(); view.id = license.getId(); view.companyId = license.getCompanyId();
        view.companyName = license.getCompanyName(); view.today = LocalDate.now(businessZone); view.lockDate = state == null ? null : date(state.getLockDate());
        view.forcedLock = state != null && state.isForcedLock(); view.hidden = license.isHidden(); view.status = status(view.forcedLock, view.lockDate);
        view.suggestedDate = suggestedDate(view.today, view.lockDate);
        view.revision = ReviewFingerprint.of(view.id.toString(), license.getClient().getId().toString(), license.getCompanyName(),
                String.valueOf(license.getCompanyId()), Boolean.toString(view.hidden), state == null ? null : state.getId().toString(), Boolean.toString(view.forcedLock),
                state == null || state.getLockDate() == null ? null : Long.toString(state.getLockDate().getTimeInMillis()),
                state == null ? null : String.valueOf(state.getLimitTransaction())); return view;
    }
    static LocalDate suggestedDate(LocalDate today, LocalDate previous) {
        LocalDate next = today.plusMonths(1).withDayOfMonth(20);
        return previous != null && previous.isAfter(next) ? previous : next;
    }
    private void copyOption(PaymentReviewViews.LicenseOption from, PaymentReviewViews.LicenseOption to) {
        to.id = from.id; to.companyId = from.companyId; to.companyName = from.companyName; to.revision = from.revision;
        to.today = from.today; to.lockDate = from.lockDate; to.forcedLock = from.forcedLock; to.hidden = from.hidden; to.status = from.status; to.suggestedDate = from.suggestedDate;
    }
    private PaymentReviewViews.Comment commentView(Comment comment) {
        PaymentReviewViews.Comment view = new PaymentReviewViews.Comment(); view.id = comment.getId(); view.text = comment.getText();
        view.visibility = comment.getVisibility().name(); view.origin = comment.getOrigin().name(); view.occurredAt = comment.getOccurredAt();
        view.author = actorView(comment.getAuthor()); return view;
    }
    private PaymentReviewViews.Actor actorView(User user) {
        if (user == null) return null; PaymentReviewViews.Actor view = new PaymentReviewViews.Actor(); view.id = user.getId();
        view.name = String.join(" ", user.getFirstname() == null ? "" : user.getFirstname(), user.getLastname() == null ? "" : user.getLastname()).trim();
        if (view.name.isEmpty()) view.name = user.getUsername(); return view;
    }
    private User actor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof JwtUser) || auth.getPrincipal() instanceof PortalPrincipal
                || auth.getAuthorities().stream().noneMatch(role -> role.getAuthority().equals("ROLE_ADMIN")))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere acceso administrativo autenticado.");
        User user = em.find(User.class, ((JwtUser) auth.getPrincipal()).getId());
        if (user == null || user.getRemoveDate() != null || !Boolean.TRUE.equals(user.getEnabled())
                || user.getAuthorities().stream().noneMatch(role -> role.getName() == AuthorityName.ROLE_ADMIN))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Se requiere un administrador vigente."); return user;
    }
    private Payment payment(Long id) {
        Payment payment = payments.findById(id).orElseThrow(this::missing);
        if (payment.getRemoveDate() != null) throw missing(); return payment;
    }
    private LicenseStatus state(License license) {
        List<LicenseStatus> states = em.createQuery("select s from LicenseStatus s where s.license.id = :id and s.removeDate is null order by s.id", LicenseStatus.class)
                .setParameter("id", license.getId()).setMaxResults(1).getResultList(); return states.isEmpty() ? null : states.get(0);
    }
    private <T> Page<T> entities(String alias, String fetch, String count, String condition, Class<T> type, Long id, int page, int size) {
        Pageable pagination = page(page, size);
        List<T> content = em.createQuery("select " + alias + " from " + fetch + " where " + condition + " order by " + alias + ".id", type)
                .setParameter("id", id).setFirstResult(Math.toIntExact(pagination.getOffset())).setMaxResults(size).getResultList();
        Long total = em.createQuery("select count(" + alias + ") from " + count + " where " + condition, Long.class).setParameter("id", id).getSingleResult();
        return new PageImpl<>(content, pagination, total);
    }
    private Pageable page(int page, int size) {
        if (page < 0 || size < 1 || size > 100 || page > Integer.MAX_VALUE / size) bad("Paginación inválida.");
        return PageRequest.of(page, size, Sort.by("id"));
    }
    private void pending(Payment payment, Long version) {
        if (payment.getStatus() != PaymentStatus.PENDIENTE || version == null || payment.getVersion() != version)
            conflict("El pago ya fue resuelto o cambió. Actualizá su detalle antes de continuar.");
    }
    private String status(boolean forced, LocalDate date) {
        // Conservar el bloqueo efectivo del flujo existente: vence al comenzar la fecha, según el servidor.
        long days = date == null ? Long.MIN_VALUE : ChronoUnit.DAYS.between(LocalDate.now(), date);
        return forced ? "BLOCKED" : date == null ? "UNKNOWN" : days <= 0 ? "EXPIRED" : days <= 15 ? "EXPIRING" : "ACTIVE";
    }
    private LocalDate date(Calendar value) { return value == null ? null : value.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(); }
    private Calendar calendar(LocalDate value) { return GregorianCalendar.from(value.atStartOfDay(ZoneId.systemDefault())); }
    private LocalDateTime now() { return LocalDateTime.now(businessZone).truncatedTo(ChronoUnit.MICROS); }
    private void bad(String text) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, text); }
    private void conflict(String text) { throw new ResponseStatusException(HttpStatus.CONFLICT, text); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el pago o la licencia disponible."); }
    private static class Change { PaymentReviewInputs.LicenseAction input; License license; LicenseStatus state; PaymentReviewViews.LicensePreview view; }
    private static class Plan { PaymentReviewViews.Preview view; List<Change> changes; }
}
