package com.dysi.AngryBoxClientManager.dtos;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/** Complete edit form, separate from persistence entities and legacy projections. */
public class ClientInput {
    @NotBlank(message = "Ingresá el nombre.") @Size(max = 100) public String firstname;
    @NotBlank(message = "Ingresá el apellido.") @Size(max = 100) public String lastname;
    @NotNull @Email(message = "Ingresá un correo válido.") @Size(max = 255) public String email;
    @NotNull @Size(max = 50) public String phone;
    @NotNull @Size(max = 32) public String cuil;
}
