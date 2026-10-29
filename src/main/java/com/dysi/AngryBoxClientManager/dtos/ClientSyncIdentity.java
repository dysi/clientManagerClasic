package com.dysi.AngryBoxClientManager.dtos;

import java.util.Calendar;

/** Proyección del padrón: evita cargar usuarios, licencias y relaciones por cliente. */
public class ClientSyncIdentity {
    public final Long id;
    public final String firstname;
    public final String lastname;
    public final String legalName;
    public final String tradeName;
    public final String email;
    public final String phone;
    public final String dni;
    public final String cuil;
    public final String externalCode;
    public final boolean removed;

    public ClientSyncIdentity(Long id, String firstname, String lastname, String legalName,
            String tradeName, String email, String phone, String dni, String cuil,
            String externalCode, Calendar removeDate) {
        this.id = id;
        this.firstname = firstname;
        this.lastname = lastname;
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.email = email;
        this.phone = phone;
        this.dni = dni;
        this.cuil = cuil;
        this.externalCode = externalCode;
        this.removed = removeDate != null;
    }

    public String name() {
        if (legalName != null && !legalName.trim().isEmpty()) {
            return legalName;
        }
        String personal = ((firstname == null ? "" : firstname) + " " + (lastname == null ? "" : lastname)).trim();
        return personal.isEmpty() && tradeName != null ? tradeName : personal;
    }
}
