package com.dysi.AngryBoxClientManager.dtos;

import com.dysi.AngryBoxClientManager.models.AuthorityName;
import java.time.LocalDate;
import java.util.List;
import javax.validation.constraints.*;

/** Input contracts for the administration UI. Persistence entities are never bound from JSON. */
public final class AdminInputs {
    private AdminInputs() { }

    public static class VisibilityInput { @NotNull public Boolean hidden; }
    public static class ClientStatusInput { @NotNull public Boolean removed; }

    public static class LicenseInput {
        @NotNull @Positive public Long clientId;
        @NotNull @Positive public Long companyId;
        @NotBlank @Size(max = 255) public String companyName;
        @NotNull @Size(max = 255) public String description;
        @NotNull public LocalDate initialActivity;
        @NotNull public LocalDate lockDate;
        @NotNull public Boolean prePayment;
    }

    public enum LicenseBatchAction { LOCK_DATE, BLOCK, UNBLOCK }

    public static class LicenseBatchInput {
        @NotEmpty @Size(max = 200) public List<@NotNull @Positive Long> ids;
        @NotNull public LicenseBatchAction action;
        public LocalDate lockDate;
    }

    /** Contrato reservado; las escrituras permanecen cerradas hasta la etapa de presentación. */
    public static class PaymentInput { }

    public static class UserInput {
        @NotBlank @Size(min = 4, max = 50) public String username;
        @NotBlank @Size(min = 4, max = 50) public String firstname;
        @NotBlank @Size(min = 4, max = 50) public String lastname;
        @NotBlank @Email @Size(max = 50) public String email;
        @NotNull public Boolean enabled;
        @NotEmpty @Size(max = 4) public List<@NotNull AuthorityName> roles;
        @Positive public Long clientId;
    }

    public static class NewUserInput extends UserInput {
        @NotBlank @Size(min = 12, max = 72) public String password;
    }

    public static class PasswordInput {
        @NotBlank @Size(min = 12, max = 72) public String password;
    }

    public static class LockInput { @NotNull public Boolean locked; }
    public static class PaymentStatusInput { @NotNull public Boolean removed; }
}
