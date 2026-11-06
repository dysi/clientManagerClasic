package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.PaymentMethod;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.validation.constraints.*;

/** El cliente, el actor, el origen y el estado nunca se reciben del navegador. */
public final class PortalInputs {
    private PortalInputs() { }

    public static class Receipts {
        @Size(max = 20) public java.util.List<@Size(max = 1000) String> descriptions;
        // Spring necesita el accessor para asociar errores de validación de elementos de la lista.
        public java.util.List<String> getDescriptions() { return descriptions; }
    }

    public static class AdditionalReceipts extends Receipts {
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,64}") public String uploadKey;
    }

    public static class Submission extends Receipts {
        @NotNull @Positive public Long bankAccountId;
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") public String accountRevision;
        @Positive public Long licenseId;
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 2)
        public BigDecimal amount;
        @NotNull public LocalDateTime issuedAt;
        @NotBlank @Size(max = 50) public String contactPhone;
        @NotNull public PaymentMethod method;
        public boolean withoutReceipt;
        @Size(max = 1000) public String noReceiptReason;
        @Size(max = 4000) public String comment;
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,64}") public String submissionKey;
    }

    public static class PublicComment {
        @NotBlank @Size(max = 4000) public String text;
    }
}
