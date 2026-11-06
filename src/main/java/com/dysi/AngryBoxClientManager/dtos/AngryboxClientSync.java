package com.dysi.AngryBoxClientManager.dtos;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.*;

/** Contrato v1 de Angrybox. Nunca aceptar entidades ni actores de auditoría desde el JSON. */
public final class AngryboxClientSync {
    private AngryboxClientSync() {
    }

    public static class Request {
        @NotNull @Min(1) @Max(1)
        public Integer versionContrato;
        @NotBlank
        @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
        public String sincronizacionId;
        @NotNull @Positive
        public Long companyId;
        @NotNull
        public OffsetDateTime fechaDatos;
        @NotNull
        public List<IncomingClient> clientes;
    }

    public static class IncomingClient {
        public Long id;
        public String nombre;
        public String mail;
        public String domicilio;
        public String celular;
        public String radio;
        public String observaciones;
        public Long tipoDocID;
        public String tipoDocDesc;
        public String cuitCuil;
        public Boolean datosFiscales;
        public String tipoContribuyente;
        public String tipoContribuyenteDesc;
        public BigDecimal deudaCuentaCorriente;
        public String codigoExterno;
        public Boolean baja;
    }

    public static class Result {
        public Long id;
        public String nombre;
        public String cuitCuil;
        public String codigoExterno;
        public BigDecimal deudaCuentaCorriente;
        public boolean sincronizo;
        public Boolean baja;
        public Long clientManagerId;
        public String criterioCoincidencia;
        public List<String> errores = new ArrayList<>();

        public Result() {
        }

        public Result(IncomingClient source) {
            id = source.id;
            nombre = source.nombre;
            cuitCuil = source.cuitCuil;
            codigoExterno = source.codigoExterno;
            deudaCuentaCorriente = source.deudaCuentaCorriente;
            baja = source.baja;
        }
    }

    public static class ExternalClient {
        public Long id;
        public String nombre;
        public String mail;
        public String domicilio;
        public String celular;
        public String observaciones;
        public String dni;
        public String cuit;
        public String codigoExterno;
    }

    public static class Response {
        public int versionContrato = 1;
        public String sincronizacionId;
        public List<Result> clientesIn = new ArrayList<>();
        public List<ExternalClient> clientesOut = new ArrayList<>();
    }
}
