package com.dysi.AngryBoxClientManager.dtos;

import javax.validation.constraints.*;

/** Contratos administrativos; nunca aceptan auditoría ni disponibilidad calculada. */
public final class ParameterInputs {
    private ParameterInputs() { }
    public static class BankInput {
        @NotBlank @Size(max = 255) public String name;
        @NotNull public Boolean active;
    }
    public static class BankAccountInput {
        @NotNull @Positive public Long bankId;
        @NotBlank @Size(max = 255) public String holder;
        // Permite pegar separadores habituales sin aceptar letras ni otra cantidad de dígitos.
        @NotBlank @Size(max = 64)
        @Pattern(regexp = "(?:[\\s-]*[0-9]){22}[\\s-]*", message = "Ingresá un CBU de 22 dígitos.")
        public String cbu;
        @Size(max = 100) public String alias;
        @Size(max = 100) public String externalCode;
        @NotNull public Boolean active;
    }
    public static class ActiveInput { @NotNull public Boolean active; }
}
