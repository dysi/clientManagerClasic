package com.dysi.AngryBoxClientManager.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Proyecciones públicas mínimas. No contienen actores ni información interna de almacenamiento. */
public final class PortalViews {
    private PortalViews() { }

    public static class Overview {
        public boolean sensitiveAccess;
        public String name, state;
        public ClientIdentity client;
        public Balance balance;
        public LicenseTotals licenses;
        public Extension extension;
    }
    public static class Extension {
        public boolean active, eligible, expiredLicenses;
        public String reason, message, origin;
        public java.math.BigDecimal durationDays;
        public java.time.Instant expiresAt;
        public LocalDate expiresOn;
    }
    public static class ClientIdentity {
        public String firstname, lastname, email, phone, dni, cuil;
    }
    public static class LicenseTotals {
        public final long total, blocked, expired, expiring, unknown;
        public LicenseTotals(Long total, Long blocked, Long expired, Long expiring, Long unknown) {
            this.total = total == null ? 0 : total; this.blocked = blocked == null ? 0 : blocked;
            this.expired = expired == null ? 0 : expired; this.expiring = expiring == null ? 0 : expiring;
            this.unknown = unknown == null ? 0 : unknown;
        }
    }
    public static class Balance {
        public BigDecimal amount;
        public String currency, state;
        public LocalDateTime synchronizedAt;
    }
    public static class License {
        public Long id, daysUntilLock;
        public String companyName, status;
        public LocalDate lockDate;
        public boolean forcedLock;
    }
    public static class Account {
        public Long id;
        public String bank, holder, cbu, alias, revision;
    }
    public static class Payment {
        public long commentCount, receiptCount;
        public Long id;
        public BigDecimal amount;
        public String currency, status, companyName, rejectionReason;
        public LocalDateTime issuedAt;
    }
    public static class PaymentDetail {
        public Payment payment;
        public Long licenseId;
        public String contactPhone, method, destinationBank, destinationHolder, destinationCbu, destinationAlias, noReceiptReason;
        public boolean withoutReceipt;
    }
    public static class Receipt {
        public Long id;
        public String originalName, description, mimeType;
        public long size;
        public LocalDateTime uploadedAt;
    }
    public static class Comment {
        public Long id;
        public String text;
        public LocalDateTime occurredAt;
    }
    public static class FormContext {
        public boolean sensitiveAccess;
        public BigDecimal suggestedAmount;
        public java.util.List<Account> accounts;
        public String contactPhone;
        public int maxReceipts;
        public long maxFileBytes;
    }
}
