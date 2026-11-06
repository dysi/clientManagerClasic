package com.dysi.AngryBoxClientManager.payments;

import com.dysi.AngryBoxClientManager.models.*;
import com.dysi.AngryBoxClientManager.security.JwtUserFactory;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import javax.persistence.EntityManager;
import javax.validation.ConstraintViolationException;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PortalModelTests {
    @Autowired EntityManager em;
    private User actor;
    private Client client;
    private License license;

    @BeforeEach void setup() {
        SecurityContextHolder.clearContext();
        actor = new User(); actor.setUsername("model-tests"); actor.setFirstname("Nombre"); actor.setLastname("Apellido");
        actor.setEmail("model@example.test"); actor.setPassword("TestHashOnly"); actor.setEnabled(true);
        actor.setAuthorities(new ArrayList<>()); em.persist(actor); em.flush();
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                JwtUserFactory.create(actor), null, Collections.emptyList()));
        client = new Client(); client.setFirstname("Prueba"); client.setCuil("dato histórico sin clasificar");
        client.setDni("00123456"); em.persist(client);
        license = new License(); license.setClient(client); license.setCompanyId(10L); em.persist(license);
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); }
    private Payment payment() { return PaymentFixtures.payment(em, client, license, actor); }

    @Test void decimalMoneyOptionalLicenseAndNoFabricatedBalance() {
        Payment payment = payment(); payment.setLicense(null); payment.setAmount(new BigDecimal("0.10"));
        em.persist(payment); em.flush(); em.clear();
        Payment saved = em.find(Payment.class, payment.getId());
        assertEquals(new BigDecimal("0.10"), saved.getAmount()); assertNull(saved.getLicense());
        assertEquals(PaymentStatus.PENDIENTE, saved.getStatus()); assertEquals(com.dysi.AngryBoxClientManager.models.Currency.ARS, saved.getCurrency());
        assertEquals(actor.getId(), saved.getCreatedBy().getId());
        assertEquals(LocalDateTime.of(2026, 10, 26, 12, 30), saved.getIssuedAt());
        assertEquals(0L, em.createQuery("select count(b) from ClientAccountBalance b", Long.class).getSingleResult());
        assertEquals("dato histórico sin clasificar", em.find(Client.class, client.getId()).getCuil());
        assertEquals("00123456", em.find(Client.class, client.getId()).getDni());
    }

    @Test void paymentRejectsLicenseOwnedByAnotherClient() {
        Client other = new Client(); em.persist(other);
        Payment payment = payment(); payment.setClient(other);
        assertThrows(IllegalStateException.class, () -> em.persist(payment));
    }

    @Test void qrIsUnavailableEvenWhenAssignedDirectly() {
        Payment payment = payment(); payment.setMethod(PaymentMethod.QR);
        assertThrows(IllegalStateException.class, () -> em.persist(payment));
    }

    @Test void zeroAmountIsInvalid() {
        Payment payment = payment(); payment.setAmount(BigDecimal.ZERO);
        assertThrows(ConstraintViolationException.class, () -> { em.persist(payment); em.flush(); });
    }

    @Test void fractionalPrecisionIsValidated() {
        Payment payment = payment(); payment.setAmount(new BigDecimal("1.001"));
        assertThrows(ConstraintViolationException.class, () -> { em.persist(payment); em.flush(); });
    }

    @Test void missingReceiptRequiresReason() {
        Payment payment = payment(); payment.setNoReceiptReason(" ");
        assertThrows(IllegalStateException.class, () -> em.persist(payment));
    }

    @Test void removingLicenseCollectionDoesNotDeletePaymentOrEvidence() {
        Payment payment = payment(); em.persist(payment);
        Comment comment = new Comment(); comment.setText("Texto <script> conservado como texto");
        comment.setVisibility(CommentVisibility.PUBLICO); comment.setAuthor(actor);
        comment.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); comment.setOccurredAt(LocalDateTime.now()); em.persist(comment);
        PaymentComment link = new PaymentComment(); link.setPayment(payment); link.setComment(comment); em.persist(link);
        Attachment attachment = new Attachment(); attachment.setStorageKey(UUID.randomUUID().toString());
        attachment.setOriginalName("prueba.pdf"); attachment.setMimeType("application/pdf"); attachment.setStoredSize(10L);
        attachment.setSha256("a".repeat(64)); attachment.setUploadedAt(LocalDateTime.now()); attachment.setUploadedBy(actor);
        attachment.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); em.persist(attachment);
        PaymentAttachment receipt = new PaymentAttachment(); receipt.setPayment(payment); receipt.setAttachment(attachment); em.persist(receipt);
        PaymentStatusHistory history = new PaymentStatusHistory(); history.setPayment(payment);
        history.setNewStatus(PaymentStatus.PENDIENTE); history.setOccurredAt(LocalDateTime.now());
        history.setResponsibleUser(actor); history.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); em.persist(history);
        em.flush(); em.clear();
        License saved = em.find(License.class, license.getId()); assertEquals(1, saved.getPaymentList().size());
        saved.getPaymentList().clear(); em.flush(); em.clear();
        assertNotNull(em.find(Payment.class, payment.getId())); assertNotNull(em.find(Comment.class, comment.getId()));
        assertNotNull(em.find(Attachment.class, attachment.getId())); assertNotNull(em.find(PaymentStatusHistory.class, history.getId()));
        payment = em.find(Payment.class, payment.getId()); payment.setLicense(null); em.flush();
        em.remove(em.find(License.class, license.getId())); em.flush(); em.clear();
        assertNotNull(em.find(Payment.class, payment.getId())); assertNotNull(em.find(PaymentComment.class, link.getId()));
        assertNotNull(em.find(PaymentAttachment.class, receipt.getId()));
    }

    @Test void bankChangesDoNotRewriteDestinationSnapshot() {
        Payment payment = payment(); em.persist(payment); em.flush();
        String oldHolder = payment.getDestinationHolder(); payment.getBankAccount().setHolder("Otro titular");
        em.flush(); em.clear(); assertEquals(oldHolder, em.find(Payment.class, payment.getId()).getDestinationHolder());
    }

    @Test void bankNormalizationAndBalanceKeepSignedDecimalValue() {
        Bank bank = new Bank(); bank.setName("  Banco   PRUEBA  "); em.persist(bank);
        BankAccount account = new BankAccount(); account.setBank(bank); account.setHolder(" Titular ");
        account.setCbu("00000000000-00000000000"); account.setAlias(" ALIAS.Prueba "); em.persist(account);
        ClientAccountBalance balance = new ClientAccountBalance(); balance.setClient(client);
        balance.setAmount(new BigDecimal("-10.25")); balance.setSynchronizedAt(LocalDateTime.now()); em.persist(balance);
        em.flush(); em.clear();
        assertEquals("banco prueba", em.find(Bank.class, bank.getId()).getNormalizedName());
        assertEquals("0000000000000000000000", em.find(BankAccount.class, account.getId()).getCbu());
        assertEquals("alias.prueba", em.find(BankAccount.class, account.getId()).getAlias());
        assertEquals(new BigDecimal("-10.25"), em.find(ClientAccountBalance.class, balance.getId()).getAmount());
        assertEquals(actor.getId(), em.find(ClientAccountBalance.class, balance.getId()).getCreatedBy().getId());
    }

    @Test void documentOriginCannotCreatePrivateComment() {
        Comment comment = new Comment(); comment.setText("Privado"); comment.setVisibility(CommentVisibility.PRIVADO);
        comment.setAuthor(actor); comment.setOccurredAt(LocalDateTime.now()); comment.setOrigin(OperationOrigin.PORTAL_DOCUMENTO);
        assertThrows(IllegalStateException.class, () -> em.persist(comment));
    }

    @Test void invalidHistoryTransitionsAreRejected() {
        Payment payment = payment(); em.persist(payment);
        PaymentStatusHistory history = new PaymentStatusHistory(); history.setPayment(payment);
        history.setNewStatus(PaymentStatus.APROBADO); history.setOccurredAt(LocalDateTime.now());
        history.setResponsibleUser(actor); history.setOrigin(OperationOrigin.ACCESO_AUTENTICADO);
        assertThrows(IllegalStateException.class, () -> em.persist(history));
    }
    @Test void aBalanceCannotBeDuplicatedForTheSameClient() {
        for (int index = 0; index < 2; index++) {
            ClientAccountBalance balance = new ClientAccountBalance(); balance.setClient(client);
            balance.setAmount(BigDecimal.ZERO); balance.setSynchronizedAt(LocalDateTime.now());
            if (index == 0) { em.persist(balance); em.flush(); }
            else assertThrows(javax.persistence.PersistenceException.class, () -> { em.persist(balance); em.flush(); });
        }
    }

    @Test void evidenceCannotBeSilentlyEditedOrRemoved() {
        Comment comment = new Comment(); comment.setText("Texto original"); comment.setVisibility(CommentVisibility.PUBLICO);
        comment.setAuthor(actor); comment.setOccurredAt(LocalDateTime.now()); comment.setOrigin(OperationOrigin.ACCESO_AUTENTICADO);
        em.persist(comment); em.flush();
        comment.setText("Texto reemplazado");
        assertThrows(IllegalStateException.class, () -> em.flush());
    }

    @Test void ordinaryPaymentDeletionIsRejected() {
        Payment payment = payment(); em.persist(payment); em.flush();
        assertThrows(IllegalStateException.class, () -> em.remove(payment));
    }

    @Test void stalePaymentVersionCannotOverwriteConcurrentWrite() {
        Payment payment = payment(); em.persist(payment); em.flush();
        em.createNativeQuery("update payment set version = version + 1 where id = :id")
                .setParameter("id", payment.getId()).executeUpdate();
        payment.setContactPhone("1111111111");
        assertThrows(javax.persistence.OptimisticLockException.class, () -> em.flush());
    }

}

