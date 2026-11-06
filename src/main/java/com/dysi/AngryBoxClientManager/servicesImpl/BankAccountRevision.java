package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.BankAccount;
import com.dysi.AngryBoxClientManager.models.Payment;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/** Detecta que los datos bancarios revisados cambiaron mientras se completaba el formulario. */
final class BankAccountRevision {
    private BankAccountRevision() { }
    /** Incluye explícitamente el ID enviado además de la revisión para detectar claves reutilizadas con otra cuenta. */
    static String submissionHash(Long accountId, String revision) {
        return ReviewFingerprint.of(accountId.toString(), revision);
    }
    static String submissionHash(Payment payment) {
        return submissionHash(payment.getBankAccount().getId(), of(payment));
    }
    static String of(BankAccount account) {
        return hash(account.getId(), account.getBank().getName(), account.getHolder(), account.getCbu(), account.getAlias());
    }
    static String of(Payment payment) {
        return hash(payment.getBankAccount().getId(), payment.getDestinationBank(), payment.getDestinationHolder(),
                payment.getDestinationCbu(), payment.getDestinationAlias());
    }
    private static String hash(Object... parts) {
        StringBuilder content = new StringBuilder();
        for (Object part : parts) {
            String value = Objects.toString(part, "");
            content.append(value.length()).append(':').append(value);
        }
        try {
            StringBuilder hex = new StringBuilder(64);
            for (byte value : MessageDigest.getInstance("SHA-256").digest(content.toString().getBytes(StandardCharsets.UTF_8))) {
                hex.append(String.format("%02x", value & 255));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }
}
