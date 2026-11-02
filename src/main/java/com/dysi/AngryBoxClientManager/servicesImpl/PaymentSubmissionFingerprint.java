package com.dysi.AngryBoxClientManager.servicesImpl;

import com.dysi.AngryBoxClientManager.models.Payment;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

/** Campos corregibles normalizados igual que en la presentación del portal. */
final class PaymentSubmissionFingerprint {
    private PaymentSubmissionFingerprint() { }
    static String of(Payment payment) {
        return of(payment.getLicense() == null ? null : payment.getLicense().getId(),
                payment.getAmount(), payment.getIssuedAt(), payment.getContactPhone());
    }
    static String of(Long licenseId, BigDecimal amount, LocalDateTime issuedAt, String phone) {
        return ReviewFingerprint.of(licenseId == null ? null : licenseId.toString(),
                amount.stripTrailingZeros().toPlainString(), issuedAt.truncatedTo(ChronoUnit.MICROS).toString(), phone.trim());
    }
}
