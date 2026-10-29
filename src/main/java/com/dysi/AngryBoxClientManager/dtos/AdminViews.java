package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.AuthorityName;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Explicit output contracts. No password hashes, entity graphs or internal status lists are exposed. */
public final class AdminViews {
    private AdminViews() { }

    public static class Option {
        public final Long id;
        public final String label;
        public Option(Long id, String label) { this.id = id; this.label = label; }
    }

    public static class LicenseView {
        public Long id, clientId, companyId;
        public String clientName, companyName, description, status;
        public LocalDate initialActivity, lockDate;
        public Long daysUntilLock;
        public boolean prePayment, locked, hidden, clientRemoved;
    }

    public static class PaymentCounts {
        public long total, pending, approved, rejected;
    }

    public static class PaymentView {
        public Long id, clientId, licenseId;
        public String clientName, companyName, currency, status;
        public BigDecimal amount;
        public java.time.LocalDateTime issuedAt;
    }

    public static class UserView {
        public Long id, clientId;
        public String username, firstname, lastname, email, clientName;
        public boolean enabled;
        public List<AuthorityName> roles;
    }
}
