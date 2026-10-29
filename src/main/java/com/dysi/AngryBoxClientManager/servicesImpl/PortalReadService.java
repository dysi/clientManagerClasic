package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.dtos.PortalViews;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import com.dysi.AngryBoxClientManager.storage.PortalFileProperties;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class PortalReadService {
    private final PortalAccessService access;
    private final ClientRepository clients;
    private final LicenseRepository licenses;
    private final BankAccountRepository accounts;
    private final ClientAccountBalanceRepository balances;
    private final PaymentRepository payments;
    private final PaymentStatusHistoryRepository histories;
    private final PaymentCommentRepository comments;
    private final PortalFileProperties limits;
    private final PaymentAttachmentRepository attachments;

    public PortalReadService(PortalAccessService access, ClientRepository clients, LicenseRepository licenses,
            BankAccountRepository accounts, ClientAccountBalanceRepository balances, PaymentRepository payments,
            PaymentStatusHistoryRepository histories, PaymentCommentRepository comments, PortalFileProperties limits, PaymentAttachmentRepository attachments) {
        this.access = access; this.clients = clients; this.licenses = licenses; this.accounts = accounts;
        this.balances = balances; this.payments = payments; this.histories = histories; this.comments = comments; this.limits = limits; this.attachments = attachments;
    }

    public PortalViews.Overview overview(PortalPrincipal principal) {
        Long clientId = access.revalidate(principal).getClientId();
        Client client = clients.findById(clientId).orElseThrow(this::missing);
        PortalViews.Overview view = new PortalViews.Overview();
        view.name = client.getFirstname() == null ? "Cliente" : client.getFirstname();
        view.client = new PortalViews.ClientIdentity();
        view.client.firstname = client.getFirstname(); view.client.lastname = client.getLastname();
        view.sensitiveAccess = principal.getOrigin() == OperationOrigin.ACCESO_AUTENTICADO;
        if (view.sensitiveAccess) {
            view.client.email = client.getEmail(); view.client.phone = client.getPhone();
            view.client.dni = client.getDni(); view.client.cuil = client.getCuil();
        }
        if (view.sensitiveAccess) {
            LocalDate today = LocalDate.now();
            view.licenses = licenses.portalTotals(clientId, calendar(today.plusDays(1)), calendar(today.plusDays(16)));
            view.state = view.licenses.total == 0 ? "SIN_LICENCIAS"
                    : view.licenses.blocked + view.licenses.expired > 0 ? "CON_RESTRICCIONES"
                    : view.licenses.unknown > 0 ? "REVISAR_DATOS" : view.licenses.expiring > 0 ? "POR_VENCER" : "VIGENTE";
        }
        balances.findByClientIdAndRemoveDateIsNull(clientId).ifPresent(balance -> {
            PortalViews.Balance output = new PortalViews.Balance(); output.amount = balance.getAmount();
            output.currency = balance.getCurrency().name(); output.synchronizedAt = balance.getSynchronizedAt();
            output.state = balance.getAmount().signum() > 0 ? "DEUDA" : balance.getAmount().signum() < 0 ? "A_FAVOR" : "SIN_DEUDA";
            view.balance = output;
        });
        return view;
    }

    public Page<PortalViews.License> licenses(PortalPrincipal principal, String keyword, int page, int size) {
        access.requireSensitiveAccess(principal);
        Long clientId = access.revalidate(principal).getClientId();
        return licenses.findAll(ownLicenses(clientId, keyword), page(page, size, "companyName", "id"))
                .map(license -> {
                    com.dysi.AngryBoxClientManager.dtos.AdminViews.LicenseView source = WebAdminService.licenseView(license);
                    PortalViews.License view = new PortalViews.License(); view.id = source.id; view.companyName = source.companyName;
                    view.status = source.status; view.lockDate = source.lockDate; view.daysUntilLock = source.daysUntilLock;
                    view.forcedLock = source.locked; return view;
                });
    }

    public Page<PortalViews.Account> accounts(PortalPrincipal principal, int page, int size) {
        access.revalidate(principal);
        return accounts.findAll(availableAccounts(), page(page, size, "bank.name", "id")).map(this::accountView);
    }

    private Specification<BankAccount> availableAccounts() {
        return (root, query, cb) -> cb.and(cb.isTrue(root.get("active")), cb.isNull(root.get("removeDate")),
                cb.isTrue(root.get("bank").get("active")), cb.isNull(root.get("bank").get("removeDate")));
    }

    private PortalViews.Account accountView(BankAccount account) {
        PortalViews.Account view = new PortalViews.Account(); view.id = account.getId();
        view.bank = account.getBank().getName(); view.holder = account.getHolder();
        view.cbu = account.getCbu(); view.alias = account.getAlias(); view.revision = BankAccountRevision.of(account); return view;
    }

    public PortalViews.FormContext form(PortalPrincipal principal) {
        Long clientId = access.revalidate(principal).getClientId();
        PortalViews.FormContext form = new PortalViews.FormContext();
        form.sensitiveAccess = principal.getOrigin() == OperationOrigin.ACCESO_AUTENTICADO;
        if (form.sensitiveAccess) form.contactPhone = clients.findById(clientId).orElseThrow(this::missing).getPhone();
        // Es un catálogo pequeño de cuentas disponibles; el formulario lo presenta completo.
        form.accounts = accounts.findAll(availableAccounts(), Sort.by("bank.name", "id")).stream()
                .map(this::accountView).collect(Collectors.toList());
        // Solo una deuda positiva es sugerencia de pago; no convertir crédito o ausencia en deuda.
        balances.findByClientIdAndRemoveDateIsNull(clientId).filter(balance -> balance.getAmount().signum() > 0)
                .ifPresent(balance -> form.suggestedAmount = balance.getAmount());
        form.maxReceipts = limits.getMaxReceipts(); form.maxFileBytes = limits.getMaxFileBytes(); return form;
    }

    public Page<PortalViews.Payment> payments(PortalPrincipal principal, int page, int size) {
        Long clientId = access.revalidate(principal).getClientId();
        LocalDateTime now = LocalDateTime.now();
        Page<Payment> results = payments.findAll((Specification<Payment>) (root, query, cb) -> cb.and(
                cb.equal(root.get("client").get("id"), clientId), cb.isNull(root.get("removeDate")),
                cb.between(root.get("issuedAt"), now.minusDays(45), now)),
                PageRequest.of(page(page, size, "id").getPageNumber(), size, Sort.by(Sort.Direction.DESC, "id")));
        List<Long> ids = results.stream().map(Payment::getId).collect(Collectors.toList());
        Map<Long, String> rejectionReasons = ids.isEmpty() ? Map.of() : histories.findByPaymentIdInAndNewStatus(ids, PaymentStatus.RECHAZADO)
                .stream().collect(Collectors.toMap(event -> event.getPayment().getId(), PaymentStatusHistory::getReason));
        Map<Long, Long> counts = new HashMap<>();
        if (!ids.isEmpty()) for (Object[] row : comments.publicCounts(ids)) counts.put((Long) row[0], (Long) row[1]);
        Map<Long, Long> receiptCounts = new HashMap<>();
        if (!ids.isEmpty()) for (Object[] row : attachments.activeCounts(ids)) receiptCounts.put((Long) row[0], (Long) row[1]);
        return results.map(payment -> {
            PortalViews.Payment view = paymentView(payment, rejectionReasons.get(payment.getId()));
            view.commentCount = counts.getOrDefault(payment.getId(), 0L);
            view.receiptCount = receiptCounts.getOrDefault(payment.getId(), 0L); return view;
        });
    }

    public PortalViews.PaymentDetail detail(PortalPrincipal principal, Long id) {
        access.requireSensitiveAccess(principal);
        Long clientId = access.revalidate(principal).getClientId();
        Payment payment = payments.findByIdAndClientIdAndRemoveDateIsNull(id, clientId).orElseThrow(this::missing);
        String reason = histories.findByPaymentIdInAndNewStatus(List.of(id), PaymentStatus.RECHAZADO)
                .stream().map(PaymentStatusHistory::getReason).findFirst().orElse(null);
        PortalViews.PaymentDetail view = new PortalViews.PaymentDetail(); view.payment = paymentView(payment, reason);
        for (Object[] row : comments.publicCounts(List.of(id))) view.payment.commentCount = (Long) row[1];
        view.licenseId = payment.getLicense() == null ? null : payment.getLicense().getId();
        view.contactPhone = payment.getContactPhone(); view.method = payment.getMethod().name();
        view.destinationBank = payment.getDestinationBank(); view.destinationHolder = payment.getDestinationHolder();
        view.destinationCbu = payment.getDestinationCbu(); view.destinationAlias = payment.getDestinationAlias();
        view.withoutReceipt = payment.getWithoutReceipt(); view.noReceiptReason = payment.getNoReceiptReason(); return view;
    }

    public Page<PortalViews.Receipt> receipts(PortalPrincipal principal, Long id, int page, int size) {
        access.requireSensitiveAccess(principal);
        Long clientId = access.revalidate(principal).getClientId();
        payments.findByIdAndClientIdAndRemoveDateIsNull(id, clientId).orElseThrow(this::missing);
        return attachments.ownReceipts(id, clientId, page(page, size, "id")).map(link -> {
            Attachment file = link.getAttachment(); PortalViews.Receipt view = new PortalViews.Receipt();
            view.id = file.getId(); view.originalName = file.getOriginalName(); view.description = file.getDescription();
            view.mimeType = file.getMimeType(); view.size = file.getStoredSize(); view.uploadedAt = file.getUploadedAt(); return view;
        });
    }

    public Page<PortalViews.Comment> comments(PortalPrincipal principal, Long paymentId, int page, int size) {
        access.requireSensitiveAccess(principal);
        Long clientId = access.revalidate(principal).getClientId();
        payments.findByIdAndClientIdAndRemoveDateIsNull(paymentId, clientId).orElseThrow(this::missing);
        return comments.publicComments(paymentId, clientId, page(page, size, "id")).map(link -> {
            PortalViews.Comment view = new PortalViews.Comment(); view.id = link.getComment().getId();
            view.text = link.getComment().getText(); view.occurredAt = link.getComment().getOccurredAt(); return view;
        });
    }

    public static PortalViews.Payment paymentView(Payment payment, String rejectionReason) {
        PortalViews.Payment view = new PortalViews.Payment(); view.id = payment.getId(); view.amount = payment.getAmount();
        view.currency = payment.getCurrency().name(); view.status = payment.getStatus().name(); view.issuedAt = payment.getIssuedAt();
        view.companyName = payment.getLicense() == null ? null : payment.getLicense().getCompanyName();
        view.rejectionReason = payment.getStatus() == PaymentStatus.RECHAZADO ? rejectionReason : null; return view;
    }

    private Specification<License> ownLicenses(Long clientId, String keyword) {
        if (keyword == null || keyword.length() > 150) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Búsqueda inválida.");
        String pattern = "%" + keyword.trim().toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        return (root, query, cb) -> cb.and(cb.equal(root.get("client").get("id"), clientId),
                cb.isNull(root.get("removeDate")), cb.isFalse(root.get("hidden")), cb.like(cb.lower(cb.coalesce(root.get("companyName"), "")), pattern, '!'));
    }
    private Pageable page(int page, int size, String... sort) {
        if (page < 0 || size < 1 || size > 100) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginación inválida.");
        return PageRequest.of(page, size, Sort.by(sort));
    }
    private Calendar calendar(LocalDate date) { return GregorianCalendar.from(date.atStartOfDay(ZoneId.systemDefault())); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el registro."); }
}
