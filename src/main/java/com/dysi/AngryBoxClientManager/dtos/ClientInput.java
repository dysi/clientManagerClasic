package com.dysi.AngryBoxClientManager.dtos;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Complete edit form, separate from persistence entities and legacy projections. */
public class ClientInput {
    @Size(max = 255) public String street;
    @Size(max = 30) public String streetNumber;
    @Size(max = 255) public String addressComplement;
    @Size(max = 255) public String neighborhood;
    @Size(max = 255) public String city;
    @Size(max = 30) public String postalCode;
    @Size(max = 4000) public String observations;
    // Omitir conserva; cero limpia explícitamente la selección.
    @javax.validation.constraints.Min(0) public Long countryId;
    @javax.validation.constraints.Min(0) public Long provinceId;

    @NotBlank(message = "Ingresá el nombre.") @Size(max = 100) public String firstname;
    @NotBlank(message = "Ingresá el apellido.") @Size(max = 100) public String lastname;
    @NotNull @Email(message = "Ingresá un correo válido.") @Size(max = 255) public String email;
    @NotNull @Size(max = 50) public String phone;
    @NotNull @Size(max = 32) public String cuil;
    // Omitido conserva el valor en consumidores anteriores; vacío lo limpia.
    @javax.validation.constraints.Pattern(regexp = "[0-9]{0,8}", message = "Ingresá hasta ocho dígitos, sin separadores.")
    public String dni;
    // Omitido conserva el dato; una cadena vacía permite limpiarlo desde el ABM.
    @Size(max = 255) public String legalName;
    @Size(max = 255) public String tradeName;
    @Size(max = 100) public String externalCode;
}
