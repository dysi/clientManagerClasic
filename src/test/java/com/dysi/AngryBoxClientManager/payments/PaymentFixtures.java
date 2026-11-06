package com.dysi.AngryBoxClientManager.payments;

import com.dysi.AngryBoxClientManager.models.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import javax.persistence.EntityManager;

/** Datos sintéticos únicamente para bases desechables. */
public final class PaymentFixtures {
    private PaymentFixtures() { }

    public static Payment payment(EntityManager em, Client client, License license, User actor) {
        Bank bank = new Bank(); bank.setName("Banco " + UUID.randomUUID()); em.persist(bank);
        BankAccount account = new BankAccount(); account.setBank(bank); account.setHolder("Titular de prueba");
        String digits = String.format("%022d", Math.abs(UUID.randomUUID().getMostSignificantBits() / 10));
        account.setCbu(digits); em.persist(account);
        Payment payment = new Payment(); payment.setClient(client); payment.setLicense(license);
        payment.setAmount(new BigDecimal("100.25")); payment.setIssuedAt(LocalDateTime.of(2026, 10, 26, 12, 30));
        payment.setContactPhone("0000000000"); payment.setBankAccount(account);
        payment.setDestinationBank(bank.getName()); payment.setDestinationHolder(account.getHolder());
        payment.setDestinationCbu(account.getCbu()); payment.setDestinationAlias(account.getAlias());
        payment.setWithoutReceipt(true); payment.setNoReceiptReason("Motivo de prueba");
        payment.setOrigin(OperationOrigin.ACCESO_AUTENTICADO); payment.setSubmittedBy(actor);
        payment.setSubmissionKey(UUID.randomUUID().toString());
        return payment;
    }
}
