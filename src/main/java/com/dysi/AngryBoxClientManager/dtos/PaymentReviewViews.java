package com.dysi.AngryBoxClientManager.dtos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** Datos administrativos explícitos; nunca se reutilizan como respuestas del portal. */
public final class PaymentReviewViews {
    private PaymentReviewViews() { }
    public static class Actor { public Long id; public String name; }
    public static class Detail {
        public AdminViews.PaymentView payment;
        public long version;
        public String contactPhone, method, destinationBank, destinationHolder, destinationCbu, destinationAlias;
        public String origin, noReceiptReason;
        public boolean withoutReceipt;
        public LocalDateTime createdAt;
        public Actor submittedBy;
        public LicenseOption associatedLicense;
    }
    public static class Comment {
        public Long id;
        public String text, visibility, origin;
        public LocalDateTime occurredAt;
        public Actor author;
    }
    public static class Receipt extends PortalViews.Receipt {
        public Actor uploadedBy;
        public String origin;
    }
    public static class History {
        public Long id;
        public String previousStatus, newStatus, reason, origin;
        public LocalDateTime occurredAt;
        public Actor responsible;
    }
    public static class LicenseEvent {
        public Long id, licenseId;
        public String companyName, actionType, previousValue, newValue;
        public LocalDateTime occurredAt;
        public Actor responsible;
    }
    public static class LicenseOption {
        public Long id, companyId;
        public String companyName, revision, status;
        public boolean forcedLock;
        public LocalDate today, lockDate, suggestedDate;
    }
    public static class LicensePreview extends LicenseOption {
        public boolean unlock, updateExpiry, shortening;
        public LocalDate proposedDate;
        public Long daysFromToday, differenceFromPrevious;
        public String resultingStatus;
    }
    public static class Preview {
        public LocalDate today;
        public String token;
        public List<LicensePreview> licenses;
    }
}
