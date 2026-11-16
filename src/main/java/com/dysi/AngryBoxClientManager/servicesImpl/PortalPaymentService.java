package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.mail.PaymentMailAlert;
import com.dysi.AngryBoxClientManager.dtos.PortalInputs;
import com.dysi.AngryBoxClientManager.dtos.PortalViews;
import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.repositories.*;
import com.dysi.AngryBoxClientManager.security.PortalPrincipal;
import com.dysi.AngryBoxClientManager.storage.*;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

/** Presentación inmutable y atómica del pago, sus evidencias y el evento inicial. */
@Service
public class PortalPaymentService {
    private static final Logger LOG = LoggerFactory.getLogger(PortalPaymentService.class);
    private final PaymentMailAlert mailAlert;
    private final LicenseExtensionService extensions;
    private final PortalAccessService access;
    private final ClientRepository clients;
    private final PaymentRepository payments;
    private final EntityManager em;
    private final ReceiptProcessor processor;
    private final ReceiptStorage storage;
    private final TransactionTemplate transaction;

    public PortalPaymentService(PortalAccessService access, ClientRepository clients, PaymentRepository payments,
            EntityManager em, ReceiptProcessor processor, ReceiptStorage storage, PlatformTransactionManager manager,
            PaymentMailAlert mailAlert, LicenseExtensionService extensions) {
        this.extensions = extensions;
        this.mailAlert = mailAlert;
        this.access = access; this.clients = clients; this.payments = payments; this.em = em;
        this.processor = processor; this.storage = storage;
        transaction = new TransactionTemplate(manager); transaction.setTimeout(120);
        // Tras esperar el bloqueo de cliente, ver el pago que confirmó el primer envío
        // también en MySQL, cuya configuración habitual usa REPEATABLE_READ.
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
    }

    public PortalViews.Payment submit(PortalPrincipal principal, PortalInputs.Submission input, List<MultipartFile> files) {
        access.revalidate(principal);
        validate(input, files.size());
        validateDescriptions(input, files.size());
        // La decodificación ocurre antes de tomar bloqueos de base de datos.
        List<PreparedReceipt> receipts = new ArrayList<>(processor.prepare(files));
        AtomicBoolean enlisted = new AtomicBoolean(false);
        try {
            return transaction.execute(status -> {
                enlist(receipts); enlisted.set(true);
                return create(principal, input, receipts);
            });
        } finally {
            // Si no empezó una transacción, no habrá callback que limpie los temporales.
            if (!enlisted.get()) processor.discard(receipts);
        }
    }

    private PortalViews.Payment create(PortalPrincipal principal, PortalInputs.Submission input, List<PreparedReceipt> receipts) {
        PortalPrincipal current = access.revalidate(principal);
        Client client = clients.lockPortalClient(current.getClientId()).orElseThrow(this::missing);
        // OSIV puede conservar entidades de la validación previa a procesar los archivos.
        em.refresh(client);
        em.refresh(em.find(User.class, principal.getId()));
        current = access.revalidate(principal);
        Optional<Payment> previous = payments.findByClientIdAndSubmissionKey(client.getId(), input.submissionKey);
        if (previous.isPresent()) {
            Payment payment = previous.get();
            if (!sameSubmission(payment, input, receipts)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "La presentación ya existe con otros datos. Volvé al portal y revisá los pagos informados antes de iniciar uno nuevo.");
            // No promover los nuevos temporales de un reenvío; la evidencia original permanece.
            processor.discard(receipts);
            receipts.clear();
            return PortalReadService.paymentView(payment, rejectionReason(payment));
        }
        BankAccount account = em.find(BankAccount.class, input.bankAccountId, LockModeType.PESSIMISTIC_READ);
        if (account == null || account.getRemoveDate() != null) throw missing();
        Bank bank = em.find(Bank.class, account.getBank().getId(), LockModeType.PESSIMISTIC_READ);
        if (!account.getActive() || bank == null || !bank.getActive() || bank.getRemoveDate() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La cuenta destinataria ya no está disponible. Elegí otra cuenta.");
        }
        if (!BankAccountRevision.of(account).equals(input.accountRevision)) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Los datos de la cuenta cambiaron. Volvé al primer paso, actualizá las cuentas y revisá la destinataria.");
        License license = input.licenseId == null ? null : em.find(License.class, input.licenseId);
        if (input.licenseId != null && (license == null || license.getRemoveDate() != null || license.getClient() == null
                || !Objects.equals(license.getClient().getId(), client.getId()))) throw missing();
        LocalDateTime now = LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
        User actor = em.getReference(User.class, current.getId());
        Payment payment = new Payment(); payment.setClient(client); payment.setLicense(license);
        payment.setAmount(input.amount); payment.setIssuedAt(input.issuedAt.truncatedTo(ChronoUnit.MICROS));
        payment.setContactPhone(input.contactPhone.trim()); payment.setMethod(PaymentMethod.TRANSFERENCIA);
        payment.setBankAccount(account); payment.setDestinationBank(bank.getName()); payment.setDestinationHolder(account.getHolder());
        payment.setDestinationCbu(account.getCbu()); payment.setDestinationAlias(account.getAlias());
        payment.setOrigin(current.getOrigin()); payment.setSubmittedBy(actor);
        payment.setWithoutReceipt(input.withoutReceipt); payment.setNoReceiptReason(text(input.noReceiptReason));
        payment.setSubmissionKey(input.submissionKey); em.persist(payment);
        extensions.issue(payment);
        PaymentStatusHistory event = new PaymentStatusHistory(); event.setPayment(payment);
        event.setNewStatus(PaymentStatus.PENDIENTE); event.setOccurredAt(now);
        event.setResponsibleUser(actor); event.setOrigin(current.getOrigin()); em.persist(event);
        if (text(input.comment) != null) addComment(payment, text(input.comment), current, now);
        persistReceipts(payment, current, receipts, input.descriptions, null, now);
        em.flush();
        mailAlert.afterCreation(payment, receipts.size());
        return PortalReadService.paymentView(payment, null);
    }

    /** Agrega evidencia sin cambiar los datos ni el estado original del pago. */
    public void append(PortalPrincipal principal, Long id, PortalInputs.AdditionalReceipts input, List<MultipartFile> files) {
        PortalPrincipal current = access.revalidate(principal);
        payments.findByIdAndClientIdAndRemoveDateIsNull(id, current.getClientId()).orElseThrow(this::missing);
        if (files.isEmpty()) bad("Elegí al menos un comprobante para agregar.");
        validateDescriptions(input, files.size());
        List<PreparedReceipt> receipts = new ArrayList<>(processor.prepare(files));
        AtomicBoolean enlisted = new AtomicBoolean(false);
        try {
            transaction.executeWithoutResult(status -> {
                enlist(receipts); enlisted.set(true);
                PortalPrincipal actor = access.revalidate(principal);
                Client client = clients.lockPortalClient(actor.getClientId()).orElseThrow(this::missing);
                em.refresh(client); em.refresh(em.find(User.class, principal.getId()));
                actor = access.revalidate(principal);
                Payment payment = payments.findByIdAndClientIdAndRemoveDateIsNull(id, actor.getClientId()).orElseThrow(this::missing);
                Long count = em.createQuery("select count(pa) from PaymentAttachment pa where pa.payment.id = :id and pa.uploadKey = :key", Long.class)
                        .setParameter("id", id).setParameter("key", input.uploadKey).getSingleResult();
                if (count > 0) {
                    if (!sameReceipts(payment, input.uploadKey, receipts, input.descriptions)) throw new ResponseStatusException(
                            HttpStatus.CONFLICT, "Ese envío de comprobantes ya existe con otros datos. Revisá el detalle del pago.");
                    processor.discard(receipts); receipts.clear(); return;
                }
                persistReceipts(payment, actor, receipts, input.descriptions, input.uploadKey, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
                em.flush();
            });
        } finally { if (!enlisted.get()) processor.discard(receipts); }
    }

    private void validateDescriptions(PortalInputs.Receipts input, int count) {
        if (input.descriptions != null && input.descriptions.size() != count) bad("Cada descripción debe corresponder a un archivo del envío.");
    }

    private void persistReceipts(Payment payment, PortalPrincipal current, List<PreparedReceipt> receipts,
            List<String> descriptions, String uploadKey, LocalDateTime now) {
        for (int i = 0; i < receipts.size(); i++) {
            PreparedReceipt receipt = receipts.get(i);
            Attachment attachment = new Attachment(); attachment.setStorageKey(receipt.key);
            attachment.setOriginalName(receipt.originalName); attachment.setMimeType(receipt.mimeType);
            attachment.setStoredSize(receipt.storedSize); attachment.setSha256(receipt.sha256);
            attachment.setDescription(descriptions == null ? null : text(descriptions.get(i)));
            attachment.setUploadedAt(now); attachment.setUploadedBy(em.getReference(User.class, current.getId())); attachment.setOrigin(current.getOrigin());
            attachment.setWidth(receipt.width); attachment.setHeight(receipt.height); em.persist(attachment);
            PaymentAttachment link = new PaymentAttachment(); link.setPayment(payment); link.setAttachment(attachment);
            link.setUploadKey(uploadKey); em.persist(link);
        }
    }

    private boolean sameReceipts(Payment payment, String uploadKey, List<PreparedReceipt> receipts, List<String> descriptions) {
        javax.persistence.TypedQuery<PaymentAttachment> query = em.createQuery("select pa from PaymentAttachment pa join fetch pa.attachment "
                + "where pa.payment.id = :id and " + (uploadKey == null ? "pa.uploadKey is null" : "pa.uploadKey = :key"), PaymentAttachment.class)
                .setParameter("id", payment.getId());
        if (uploadKey != null) query.setParameter("key", uploadKey);
        List<List<String>> stored = query.getResultList().stream().map(link -> Arrays.asList(
                link.getAttachment().getSha256(), text(link.getAttachment().getDescription()))).collect(Collectors.toList());
        for (int i = 0; i < receipts.size(); i++) {
            if (!stored.remove(Arrays.asList(receipts.get(i).sha256, descriptions == null ? null : text(descriptions.get(i))))) return false;
        }
        return stored.isEmpty();
    }

    @Transactional
    public PortalViews.Comment comment(PortalPrincipal principal, Long paymentId, PortalInputs.PublicComment input) {
        PortalPrincipal current = access.revalidate(principal);
        Payment payment = payments.findByIdAndClientIdAndRemoveDateIsNull(paymentId, current.getClientId()).orElseThrow(this::missing);
        Comment comment = addComment(payment, input.text.trim(), current, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        em.flush();
        PortalViews.Comment view = new PortalViews.Comment(); view.id = comment.getId();
        view.text = comment.getText(); view.occurredAt = comment.getOccurredAt(); return view;
    }

    private Comment addComment(Payment payment, String value, PortalPrincipal principal, LocalDateTime now) {
        Comment comment = new Comment(); comment.setText(value); comment.setVisibility(CommentVisibility.PUBLICO);
        comment.setOccurredAt(now); comment.setAuthor(em.getReference(User.class, principal.getId()));
        comment.setOrigin(principal.getOrigin()); em.persist(comment);
        PaymentComment link = new PaymentComment(); link.setPayment(payment); link.setComment(comment); em.persist(link);
        return comment;
    }

    private boolean sameSubmission(Payment payment, PortalInputs.Submission input, List<PreparedReceipt> receipts) {
        boolean sameReceipts = sameReceipts(payment, null, receipts, input.descriptions);
        List<String> originalComments = em.createQuery("select pc.comment.text from PaymentComment pc "
                + "where pc.payment.id = :id and pc.comment.occurredAt = (select h.occurredAt from PaymentStatusHistory h "
                + "where h.payment.id = :id and h.previousStatus is null) order by pc.id", String.class)
                .setParameter("id", payment.getId()).setMaxResults(1).getResultList();
        return Objects.equals(payment.getOriginalDestinationHash() == null ? BankAccountRevision.submissionHash(payment)
                : payment.getOriginalDestinationHash(), BankAccountRevision.submissionHash(input.bankAccountId, input.accountRevision))
                && Objects.equals(payment.getOriginalEditableHash() == null ? PaymentSubmissionFingerprint.of(payment)
                        : payment.getOriginalEditableHash(), PaymentSubmissionFingerprint.of(input.licenseId, input.amount, input.issuedAt, input.contactPhone))
                && payment.getMethod() == input.method
                && payment.getWithoutReceipt() == input.withoutReceipt
                && Objects.equals(payment.getNoReceiptReason(), text(input.noReceiptReason)) && sameReceipts
                && Objects.equals(originalComments.isEmpty() ? null : originalComments.get(0), text(input.comment));
    }

    private String rejectionReason(Payment payment) {
        if (payment.getStatus() != PaymentStatus.RECHAZADO) return null;
        List<String> reasons = em.createQuery("select h.reason from PaymentStatusHistory h where h.payment.id = :id and h.newStatus = :state", String.class)
                .setParameter("id", payment.getId()).setParameter("state", PaymentStatus.RECHAZADO).setMaxResults(1).getResultList();
        return reasons.isEmpty() ? null : reasons.get(0);
    }

    private void validate(PortalInputs.Submission input, int fileCount) {
        if (input.method != PaymentMethod.TRANSFERENCIA) bad("QR todavía no está disponible. Usá transferencia.");
        if (input.withoutReceipt) {
            if (fileCount != 0 || text(input.noReceiptReason) == null) bad("Elegí comprobantes o completá el motivo de ausencia, sin combinar ambas alternativas.");
        } else if (fileCount == 0 || text(input.noReceiptReason) != null) {
            bad("Adjuntá al menos un comprobante o indicá que no lo vas a subir y completá el motivo.");
        }
        if (input.issuedAt.getYear() < 1000 || input.issuedAt.getYear() > 9999) bad("La fecha de emisión no es válida.");
    }

    private void enlist(List<PreparedReceipt> receipts) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void beforeCommit(boolean readOnly) {
                try { for (PreparedReceipt receipt : receipts) storage.promote(receipt.key); }
                catch (IOException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                        "No se pudieron confirmar los comprobantes. Tus datos no se guardaron; volvé a intentar."); }
            }
            @Override public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) processor.discard(receipts);
                else if (status == STATUS_COMMITTED) {
                    for (PreparedReceipt receipt : receipts) {
                        try { storage.discardPending(receipt.key); }
                        catch (IOException error) { LOG.error("Quedó un temporal pendiente de reconciliación."); }
                    }
                }
                // Resultado desconocido: no borrar archivos posiblemente confirmados.
                // La reconciliación tardía comprueba referencias reales en la base.
            }
        });
    }
    private String text(String value) { return value == null || value.trim().isEmpty() ? null : value.trim(); }
    private void bad(String message) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
    private ResponseStatusException missing() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "No se encontró el registro."); }
}
