package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.CommentVisibility;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.*;

public final class PaymentReviewInputs {
    private PaymentReviewInputs() { }
    public static class Correction {
        @Positive public Long bankAccountId;
        @Pattern(regexp = "[a-f0-9]{64}") public String accountRevision;
        @NotNull @PositiveOrZero public Long version;
        @Positive public Long licenseId;
        @NotNull @DecimalMin(value = "0", inclusive = false) @Digits(integer = 12, fraction = 2)
        public java.math.BigDecimal amount;
        @NotNull public java.time.LocalDateTime issuedAt;
        @NotBlank @Size(max = 50) public String contactPhone;
        @NotBlank @Size(max = 1000) public String reason;
    }
    public static class CommentInput {
        @NotBlank @Size(max = 4000) public String text;
        @NotNull public CommentVisibility visibility;
    }
    public static class Rejection {
        @NotNull @PositiveOrZero public Long version;
        @NotBlank @Size(max = 1000) public String reason;
    }
    public static class LicenseAction {
        @NotNull @Positive public Long licenseId;
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") public String revision;
        public boolean unlock, updateExpiry, confirmShortening;
        public LocalDate lockDate;
        public Long getLicenseId() { return licenseId; }
        public String getRevision() { return revision; }
    }
    public static class Options {
        @NotNull @PositiveOrZero public Long version;
        public boolean reviewAllLicenses;
        @NotNull @Size(max = 100) @Valid public List<@NotNull LicenseAction> licenses = new ArrayList<>();
        // Accessors necesarios para errores de validación de elementos anidados.
        public List<LicenseAction> getLicenses() { return licenses; }
    }
    public static class Approval extends Options {
        @NotBlank @Size(max = 1000) public String reason;
        @NotNull public LocalDate reviewedOn;
        @NotBlank @Pattern(regexp = "[a-f0-9]{64}") public String previewToken;
    }
}
